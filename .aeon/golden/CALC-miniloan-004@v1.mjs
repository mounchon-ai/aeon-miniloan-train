/**
 * CALC-miniloan-004@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-022@v1 (ยอดปิดบัญชีก่อนกำหนด)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → BigInt fixed-point ทั้งหมด ไม่มี Number/float ในการคำนวณเงินหรืออัตราเลย
 *   rounding_mode    HALF_UP   → roundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ
 *   rounding_points  ปัดทันทีเฉพาะบรรทัดดอกเบี้ยค้างจ่ายและบรรทัดค่าธรรมเนียม (2 ตำแหน่งต่อบรรทัด) —
 *                    เงินต้นคงเหลือเป็น input ที่ปัดมาแล้วจากตารางผ่อน อ่านตรงๆ ไม่ปัดซ้ำที่นี่
 *                    ผลรวมทั้งสามบรรทัดไม่ปัดซ้ำอีกชั้น (ทุกตัวเป็น 2 ตำแหน่งอยู่แล้ว)
 *   residual_policy  ไม่มี — ยอดครั้งเดียว ไม่ใช่ตารางผ่อนที่มีงวดสุดท้ายดูดเศษ
 *   boundary         closing_date = last_paid_due_date (0 วัน) → ดอกเบี้ย 0.00
 *                    closing_date = last_paid_due_date + 1 วัน → ดอกเบี้ยของ 1 วันเต็มตามสูตร
 *                    closing_date < last_paid_due_date (ย้อนหลัง) → ปฏิเสธ input ทันที ไม่คำนวณต่อ
 *                    ยังไม่มีงวดไหนชำระเลย → last_paid_due_date = วันเบิกจ่ายของบัญชี (เป็นตัวเลือก input
 *                      ไม่ใช่กิ่งโค้ดแยก — ทดสอบด้วยการตั้งค่า input ตรงๆ ในเคส C6)
 *                    annual_rate = 0% → ดอกเบี้ย 0.00 เสมอไม่ว่ากี่วัน
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-004@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-004@v1.mjs" --json
 */

// ── fixed-point decimal บน BigInt (สถาปัตยกรรมเดียวกับ CALC-miniloan-001@v2.mjs) ──
const pow10 = (k) => 10n ** BigInt(k);

/** ปัด HALF_UP: ครึ่งออกจากศูนย์เสมอ (ไม่ใช่ default ของ .NET ที่ปัดไปหาเลขคู่) */
function divRoundHalfUp(num, den) {
  if (den < 0n) { num = -num; den = -den; }
  const neg = num < 0n;
  const a = neg ? -num : num;
  const q = a / den;
  const rem = a % den;
  const bumped = rem * 2n >= den ? q + 1n : q;
  return neg ? -bumped : bumped;
}

const makeFx = (scale) => {
  const ONE = pow10(scale);
  return {
    scale,
    ONE,
    fromInt: (i) => BigInt(i) * ONE,
    /** "0.25" → fixed-point, แยกส่วนจำนวนเต็ม/ทศนิยมด้วยสตริงล้วน ไม่ผ่าน float เลย */
    fromDecimalString(str) {
      const neg = str.trim().startsWith("-");
      const [i, f = ""] = str.trim().replace(/^-/, "").split(".");
      const frac = (f + "0".repeat(scale)).slice(0, scale);
      const v = BigInt(i || "0") * ONE + BigInt(frac || "0");
      return neg ? -v : v;
    },
    /** สตางค์ (จำนวนเต็ม) → fixed-point — ใช้อ่าน remaining_principal (money(2)) เข้ามาคูณ */
    fromSatang: (satang) => (BigInt(satang) * ONE) / 100n,
    mul: (a, b) => divRoundHalfUp(a * b, ONE),
    div: (a, b) => divRoundHalfUp(a * ONE, b),
    /** fixed-point → สตางค์ (จำนวนเต็ม) ด้วย HALF_UP — จุดปัดจริงของสัญญานี้ */
    toSatang: (a) => divRoundHalfUp(a * 100n, ONE),
  };
};

/** "50000.00" หรือ "50000" → สตางค์ (BigInt) ด้วยสตริงล้วน ไม่ผ่าน float — money(2) มีไม่เกิน 2 ตำแหน่งเสมอ */
function moneyToSatang(str) {
  const neg = str.trim().startsWith("-");
  const [i, f = ""] = str.trim().replace(/^-/, "").split(".");
  const frac = (f + "00").slice(0, 2);
  const v = BigInt(i || "0") * 100n + BigInt(frac || "0");
  return neg ? -v : v;
}

/** จำนวนวันจริงระหว่างสองวันที่ (ISO "YYYY-MM-DD") — UTC เท่านั้น ไม่ยุ่งกับ timezone ของเครื่อง */
function toUtcMillis(iso) {
  const [y, m, d] = iso.split("-").map(Number);
  return Date.UTC(y, m - 1, d); // เดือนต้อง 0-based ให้ Date.UTC
}
function daysBetween(fromISO, toISO) {
  return Math.round((toUtcMillis(toISO) - toUtcMillis(fromISO)) / 86_400_000);
}

// ── สัญญาการคำนวณ (CALC-miniloan-004@v1) ────────────────────────────────────
function computeEarlySettlement({ remainingPrincipal, annualRate, lastPaidDueDate, closingDate }, scale) {
  const fx = makeFx(scale);

  const days = daysBetween(lastPaidDueDate, closingDate);
  if (days < 0) {
    return { declined: true, reason: `closing_date (${closingDate}) ก่อน last_paid_due_date (${lastPaidDueDate}) — ป้อนย้อนหลัง สัญญาปฏิเสธ input ก่อนเข้าสูตร` };
  }

  const principalSatang = moneyToSatang(remainingPrincipal);
  const principalFx = fx.fromSatang(principalSatang);

  // ดอกเบี้ยค้างจ่าย = เงินต้นคงเหลือ × อัตราต่อปี × วัน / 365 — ปัดทันทีที่นี่ (จุดปัดที่ 1)
  const rateFx = fx.fromDecimalString(annualRate);
  const interestFx = fx.div(fx.mul(fx.mul(principalFx, rateFx), fx.fromInt(days)), fx.fromInt(365));
  const interestSatang = fx.toSatang(interestFx);

  // ค่าธรรมเนียมปิดก่อนกำหนด = เงินต้นคงเหลือ × 1% — ปัดทันทีที่นี่ (จุดปัดที่ 2)
  const feeFx = fx.mul(principalFx, fx.fromDecimalString("0.01"));
  const feeSatang = fx.toSatang(feeFx);

  // ยอดปิดบัญชี = ผลรวมสามบรรทัด — ไม่ปัดซ้ำ (ทุกตัวเป็นสตางค์เต็มหน่วยอยู่แล้ว)
  const totalSatang = principalSatang + interestSatang + feeSatang;

  return {
    declined: false,
    days,
    remainingPrincipalSatang: principalSatang,
    interestSatang,
    feeSatang,
    totalSatang,
  };
}

// ── การแสดงผล ───────────────────────────────────────────────────────────────
const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};

// ── กรณีทดสอบ — เลือกตามลำดับของ /req:golden ขั้นที่ 4 ──────────────────────
// ไม่มี source ชนิด sample_data ผูกกับ REQ-miniloan-004 จึงไม่มีแถวของลูกค้าให้ใส่
const CASES = [
  { id: "C1", remainingPrincipal: "50000.00", annualRate: "0.25",
    lastPaidDueDate: "2026-06-15", closingDate: "2026-06-25",
    note: "กรณีปกติ — ช่วงนับ 10 วัน ใช้ช่วงเวลาเดียวกับ EX-miniloan-074 (15 มิ.ย. → 25 มิ.ย.)" },
  { id: "C2", remainingPrincipal: "50000.00", annualRate: "0.25",
    lastPaidDueDate: "2026-06-15", closingDate: "2026-06-15",
    note: "boundary — ปิดตรงวันครบกำหนดที่เพิ่งชำระพอดี ช่วงนับยาว 0 วัน (EX-miniloan-075) → ดอกเบี้ยต้องเป็น 0.00 เป๊ะ" },
  { id: "C3", remainingPrincipal: "50000.00", annualRate: "0.25",
    lastPaidDueDate: "2026-06-15", closingDate: "2026-06-16",
    note: "boundary — ขอบอีกฝั่งของ 0 วัน คือ 1 วันถัดมา (EX-miniloan-076)" },
  { id: "C4", remainingPrincipal: "50000.00", annualRate: "0.25",
    lastPaidDueDate: "2026-06-15", closingDate: "2026-06-14",
    expectDeclined: true,
    note: "boundary — closing_date ป้อนย้อนหลังก่อน last_paid_due_date หนึ่งวัน ต้องถูกปฏิเสธก่อนเข้าสูตร" },
  { id: "C5", remainingPrincipal: "50000.00", annualRate: "0",
    lastPaidDueDate: "2026-06-15", closingDate: "2026-07-15",
    note: "boundary — อัตราดอกเบี้ย 0% ต่อปี ช่วงนับ 30 วัน → ดอกเบี้ยต้องเป็น 0.00 เสมอไม่ว่ากี่วัน" },
  { id: "C6", remainingPrincipal: "100000.00", annualRate: "0.25",
    lastPaidDueDate: "2026-01-10", closingDate: "2026-01-20",
    note: "boundary — ยังไม่มีงวดไหนถูกชำระเลย (ขอปิดบัญชีก่อนงวดแรกถึงกำหนด) จึงตั้ง last_paid_due_date = วันเบิกจ่าย (2026-01-10) ตรงๆ ตามที่ตกลงไว้ ไม่ใช่กิ่งโค้ดพิเศษ" },
  { id: "C7", remainingPrincipal: "73456.78", annualRate: "0.18",
    lastPaidDueDate: "2026-03-01", closingDate: "2026-04-17",
    note: "ทดสอบการปัด HALF_UP จริง — เงินต้นและอัตราไม่ใช่เลขกลม (47 วัน, 18% ต่อปี) เพื่อให้เห็นว่าดอกเบี้ยไม่ลงตัวพอดีที่สตางค์" },
];

function runCase(c, scale) {
  const r = computeEarlySettlement(
    { remainingPrincipal: c.remainingPrincipal, annualRate: c.annualRate, lastPaidDueDate: c.lastPaidDueDate, closingDate: c.closingDate },
    scale,
  );
  return { case: c, result: r };
}

function summarise(run) {
  const { case: c, result: R } = run;
  const input = {
    remaining_principal: c.remainingPrincipal,
    annual_rate: c.annualRate,
    last_paid_due_date: c.lastPaidDueDate,
    closing_date: c.closingDate,
  };
  if (R.declined) {
    return { input, expected: { computed: false, declined_by: "CALC-miniloan-004@v1 boundary_behavior", reason: R.reason }, note: c.note };
  }
  return {
    input,
    expected: {
      computed: true,
      days_elapsed: R.days,
      remaining_principal: baht(R.remainingPrincipalSatang),
      accrued_interest: baht(R.interestSatang),
      early_settlement_fee: baht(R.feeSatang),
      early_settlement_amount: baht(R.totalSatang),
    },
    note: c.note,
  };
}

// ── entry ───────────────────────────────────────────────────────────────────
const argv = process.argv.slice(2);
const flag = (name, dflt) => { const i = argv.indexOf(name); return i < 0 ? dflt : argv[i + 1]; };
const WORKING_SCALE = Number(flag("--scale", 30));

if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map((c) => summarise(runCase(c, WORKING_SCALE))), null, 2));
  process.exit(0);
}

console.log(`CALC-miniloan-004@v1 · ยอดปิดบัญชีก่อนกำหนด (BR-miniloan-022@v1) · working scale = ${WORKING_SCALE} ตำแหน่ง · rounding = HALF_UP\n`);
for (const c of CASES) {
  const run = runCase(c, WORKING_SCALE);
  console.log("─".repeat(78));
  console.log(`${c.id} · เงินต้นคงเหลือ = ${c.remainingPrincipal} บาท · อัตราต่อปี = ${c.annualRate} · ${c.lastPaidDueDate} → ${c.closingDate}`);
  console.log(`     ${c.note}`);
  if (run.result.declined) {
    console.log(`     → ไม่คำนวณ · ${run.result.reason}`);
    continue;
  }
  const R = run.result;
  console.log(`     ช่วงนับ ${R.days} วัน`);
  console.log(`     เงินต้นคงเหลือ            ${baht(R.remainingPrincipalSatang).padStart(14)}`);
  console.log(`     ดอกเบี้ยค้างจ่าย          ${baht(R.interestSatang).padStart(14)}`);
  console.log(`     ค่าธรรมเนียมปิดก่อนกำหนด ${baht(R.feeSatang).padStart(14)}`);
  console.log(`     ─────────────────────────────────────`);
  console.log(`     ยอดปิดบัญชีก่อนกำหนด     ${baht(R.totalSatang).padStart(14)}`);
  console.log("");
}

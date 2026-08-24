/**
 * CALC-miniloan-002@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-022@v1 (ยอดปิดบัญชีก่อนกำหนด)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → BigInt fixed-point / เศษส่วนตรง ไม่มี Number/float แตะเงินเลย
 *   rounding_mode    HALF_UP   → roundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ
 *   rounding_points  ปัดทันทีที่แต่ละองค์ประกอบคำนวณเสร็จ (ตาม BR-miniloan-035@v1):
 *                      ดอกเบี้ยค้างจ่าย = round(เงินต้นคงเหลือ × อัตราต่อปี × วันจริง/365, 2)
 *                      ค่าธรรมเนียม     = round(เงินต้นคงเหลือ × 1%, 2)
 *                      ยอดปิดบัญชี      = เงินต้นคงเหลือ + ดอกเบี้ยที่ปัดแล้ว + ค่าธรรมเนียมที่ปัดแล้ว (ไม่ปัดซ้ำ)
 *   residual_policy  ไม่มี — ยอดก้อนเดียว ไม่มีงวดให้แบ่ง
 *   boundary         เงินต้นคงเหลือ = 0 → ยอดปิดบัญชี = 0
 *                    วันจริง = 0 (ปิดตรงวันครบกำหนดที่เพิ่งชำระ) → ดอกเบี้ยค้างจ่าย = 0
 *                    วันติดลบ (closing_date ก่อน last_due_date_paid) → ไม่นิยาม เกิดไม่ได้จริง
 *                      (กันที่ BR-miniloan-025@v1 ฝั่ง API ก่อนถึงสูตรนี้ — เหมือน BR-004 กันสูตร EMI)
 *                    อัตรา 0% → ดอกเบี้ยค้างจ่าย = 0 เสมอไม่ว่าวันเท่าไร
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-002@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-002@v1.mjs" --json
 */

const pow10 = (k) => 10n ** BigInt(k);
const SCALE = 20n; // ความละเอียดภายในสำหรับ parse อัตรา — เกินพอสำหรับ rate(10)
const ONE = pow10(Number(SCALE));

/** ปัด HALF_UP: ครึ่งออกจากศูนย์เสมอ (เศษส่วนตรง num/den ไม่มี float) */
function divRoundHalfUp(num, den) {
  if (den < 0n) { num = -num; den = -den; }
  const neg = num < 0n;
  const a = neg ? -num : num;
  const q = a / den;
  const rem = a % den;
  const bumped = rem * 2n >= den ? q + 1n : q;
  return neg ? -bumped : bumped;
}

/** "0.25" → fixed-point ที่ SCALE ตำแหน่ง */
function rateToFixed(str) {
  const neg = str.trim().startsWith("-");
  const [i, f = ""] = str.trim().replace(/^-/, "").split(".");
  const frac = (f + "0".repeat(Number(SCALE))).slice(0, Number(SCALE));
  const v = BigInt(i || "0") * ONE + BigInt(frac || "0");
  return neg ? -v : v;
}

/** จำนวนวันจริงระหว่างวันที่ (UTC, ปฏิทินจริง รวมปีอธิกสุรทิน) */
function daysBetween(fromISO, toISO) {
  const a = Date.UTC(...fromISO.split("-").map((x, i) => (i === 1 ? Number(x) - 1 : Number(x))));
  const b = Date.UTC(...toISO.split("-").map((x, i) => (i === 1 ? Number(x) - 1 : Number(x))));
  return Math.round((b - a) / 86400000);
}

const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};

// ── สัญญาการคำนวณ ───────────────────────────────────────────────────────────
function computeClosing({ remainingPrincipalBaht, annualRate, lastDueDatePaid, closingDate }) {
  const days = daysBetween(lastDueDatePaid, closingDate);
  if (days < 0) {
    return { declined: true, reason: `closing_date (${closingDate}) ก่อน last_due_date_paid (${lastDueDatePaid}) — จำนวนวันติดลบ (${days}) สัญญาไม่นิยามพฤติกรรม กันไว้ที่ BR-miniloan-025@v1 ฝั่ง API ก่อนถึงสูตรนี้` };
  }

  const principalSatang = BigInt(Math.round(remainingPrincipalBaht * 100));
  const r = rateToFixed(annualRate);

  // ดอกเบี้ยค้างจ่าย = round(เงินต้นคงเหลือ × อัตรา × วัน / 365, 2) — ปัดทันทีที่คำนวณเสร็จ
  const interestSatang = divRoundHalfUp(principalSatang * r * BigInt(days), ONE * 365n);

  // ค่าธรรมเนียม = round(เงินต้นคงเหลือ × 1%, 2)
  const feeSatang = divRoundHalfUp(principalSatang * 1n, 100n);

  const closingSatang = principalSatang + interestSatang + feeSatang;

  return {
    declined: false,
    days,
    principalSatang,
    interestSatang,
    feeSatang,
    closingSatang,
  };
}

// ── กรณีทดสอบ — เลือกตามลำดับของ /req:golden ขั้นที่ 4 ──────────────────────
// ไม่มี source ชนิด sample_data ในสเปกนี้ จึงไม่มีแถวของลูกค้าให้ใส่
const CASES = [
  { id: "C1", remainingPrincipalBaht: 90000, annualRate: "0.25", lastDueDatePaid: "2026-06-15", closingDate: "2026-06-25",
    note: "กรณีปกติ — เดียวกับ EX-miniloan-074 (10 วัน จาก 15 มิ.ย. ถึง 25 มิ.ย.)" },
  { id: "C2", remainingPrincipalBaht: 90000, annualRate: "0.25", lastDueDatePaid: "2026-06-15", closingDate: "2026-06-15",
    note: "boundary วันจริง = 0 — เดียวกับ EX-miniloan-075 (ปิดตรงวันครบกำหนดที่เพิ่งชำระ)" },
  { id: "C3", remainingPrincipalBaht: 90000, annualRate: "0.25", lastDueDatePaid: "2026-06-15", closingDate: "2026-06-16",
    note: "boundary วันจริง = 1 — เดียวกับ EX-miniloan-076 (ฝั่งตรงข้ามของ 0 วัน)" },
  { id: "C4", remainingPrincipalBaht: 0, annualRate: "0.25", lastDueDatePaid: "2026-06-15", closingDate: "2026-06-25",
    note: "boundary เงินต้นคงเหลือ = 0 — บัญชีผ่อนหมดพอดี" },
  { id: "C5", remainingPrincipalBaht: 90000, annualRate: "0", lastDueDatePaid: "2026-06-15", closingDate: "2026-06-25",
    note: "boundary อัตรา 0% — ดอกเบี้ยค้างจ่ายต้องเป็น 0 ไม่ว่าจำนวนวันเท่าไร" },
  { id: "C6", remainingPrincipalBaht: 90000, annualRate: "0.25", lastDueDatePaid: "2026-06-15", closingDate: "2026-06-10",
    note: "boundary วันติดลบ — เดียวกับ EX-miniloan-151 (สัญญาไม่นิยาม กันที่ BR-miniloan-025@v1 ฝั่ง API)" },
];

function summarise(c) {
  const R = computeClosing(c);
  if (R.declined) {
    return {
      input: { remaining_principal: c.remainingPrincipalBaht, annual_rate: c.annualRate, last_due_date_paid: c.lastDueDatePaid, closing_date: c.closingDate },
      expected: { computed: false, declined_by: "BR-miniloan-025@v1 (API-level, ก่อนถึงสูตร)", reason: R.reason },
      note: c.note,
    };
  }
  return {
    input: { remaining_principal: c.remainingPrincipalBaht, annual_rate: c.annualRate, last_due_date_paid: c.lastDueDatePaid, closing_date: c.closingDate },
    expected: {
      computed: true,
      days: R.days,
      remaining_principal: baht(R.principalSatang),
      accrued_interest: baht(R.interestSatang),
      early_closing_fee: baht(R.feeSatang),
      closing_amount: baht(R.closingSatang),
    },
    note: c.note,
  };
}

const argv = process.argv.slice(2);
if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map(summarise), null, 2));
  process.exit(0);
}

console.log(`CALC-miniloan-002@v1 · ยอดปิดบัญชีก่อนกำหนด · rounding = HALF_UP ทันทีที่แต่ละองค์ประกอบ\n`);
for (const c of CASES) {
  const R = computeClosing(c);
  console.log("─".repeat(78));
  console.log(`${c.id} · เงินต้นคงเหลือ ${c.remainingPrincipalBaht.toLocaleString("en-US")} บาท · อัตราต่อปี ${c.annualRate} · ${c.lastDueDatePaid} → ${c.closingDate}`);
  console.log(`     ${c.note}`);
  if (R.declined) {
    console.log(`     → ไม่คำนวณ · ${R.reason}`);
    continue;
  }
  console.log(`     จำนวนวันจริง = ${R.days}`);
  console.log(`     เงินต้นคงเหลือ      = ${baht(R.principalSatang)}`);
  console.log(`     ดอกเบี้ยค้างจ่าย     = ${baht(R.interestSatang)}`);
  console.log(`     ค่าธรรมเนียมปิดก่อนกำหนด = ${baht(R.feeSatang)}`);
  console.log(`     ยอดปิดบัญชี         = ${baht(R.closingSatang)}`);
  console.log("");
}

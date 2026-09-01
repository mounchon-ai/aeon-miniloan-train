/**
 * CALC-miniloan-005@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-050@v2 (ค่าธรรมเนียมการโปะ 1% หักจากส่วนเกินก่อนตัดเงินต้น)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → BigInt สตางค์ล้วน ไม่มี Number/float ในการคำนวณเงินเลย
 *   rounding_mode    HALF_UP   → roundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ
 *   rounding_points  ปัดทันทีเฉพาะบรรทัดค่าธรรมเนียมการโปะ (2 ตำแหน่ง) —
 *                    เงินต้นที่ตัดได้ = ส่วนเกิน − ค่าธรรมเนียมที่ปัดแล้ว (หักเอา ไม่ปัดซ้ำ)
 *                    เงินต้นคงเหลือใหม่ = เงินต้นคงเหลือ − เงินต้นที่ตัดได้ (ลบตรงๆ ไม่ปัดซ้ำ)
 *   residual_policy  ไม่มี — ธุรกรรมครั้งเดียวต่อการโปะหนึ่งครั้ง ไม่ใช่ตารางผ่อนที่มีงวดสุดท้ายดูดเศษ
 *   boundary         overpayment น้อยมาก (0.01) → ค่าธรรมเนียม 1% ปัดเป็น 0.00
 *                    เงินต้นที่ตัดได้ = เงินต้นคงเหลือพอดี → รับ ปิดที่ 0.00
 *                    เงินต้นที่ตัดได้ > เงินต้นคงเหลือ (แม้ 1 สตางค์) → ปฏิเสธทั้งรายการ
 *                    อัตรา 1% เป็นค่าคงที่ตายตัว ไม่มีเวอร์ชัน
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-005@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-005@v1.mjs" --json
 */

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

/** "5050.51" → สตางค์ (BigInt) ด้วยสตริงล้วน ไม่ผ่าน float — money(2) มีไม่เกิน 2 ตำแหน่งเสมอ */
function moneyToSatang(str) {
  const neg = str.trim().startsWith("-");
  const [i, f = ""] = str.trim().replace(/^-/, "").split(".");
  const frac = (f + "00").slice(0, 2);
  const v = BigInt(i || "0") * 100n + BigInt(frac || "0");
  return neg ? -v : v;
}

// ── สัญญาการคำนวณ (CALC-miniloan-005@v1) ────────────────────────────────────
function computePaydown({ overpayment, remainingPrincipal }) {
  const overpaymentSatang = moneyToSatang(overpayment);
  const remainingSatang = moneyToSatang(remainingPrincipal);

  // ค่าธรรมเนียมการโปะ = ส่วนเกิน × 1% — ปัดทันทีที่นี่ (จุดปัดเดียวของสัญญานี้)
  const feeSatang = divRoundHalfUp(overpaymentSatang * 1n, 100n);

  // เงินต้นที่ตัดได้ = ส่วนเกิน − ค่าธรรมเนียมที่ปัดแล้ว — หักเอา ไม่ปัดซ้ำ
  const reductionSatang = overpaymentSatang - feeSatang;

  if (reductionSatang > remainingSatang) {
    return {
      declined: true,
      feeSatang,
      reductionSatang,
      remainingSatang,
      reason: `เงินต้นที่ตัดได้ (${(reductionSatang - remainingSatang)} สตางค์เกิน) มากกว่าเงินต้นคงเหลือ — ปฏิเสธการบันทึกทั้งรายการตาม BR-miniloan-052@v2`,
    };
  }

  const newRemainingSatang = remainingSatang - reductionSatang; // ลบตรงๆ ไม่ปัดซ้ำ

  return { declined: false, feeSatang, reductionSatang, remainingSatang, newRemainingSatang };
}

// ── การแสดงผล ───────────────────────────────────────────────────────────────
const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};

// ── กรณีทดสอบ — เลือกตามลำดับของ /req:golden ขั้นที่ 4 ──────────────────────
// ไม่มี source ชนิด sample_data ผูกกับ REQ-miniloan-004 จึงไม่มีแถวลูกค้าให้ใส่
// C3/C4 คือตัวเลขเดียวกับ EX-miniloan-148/149 ที่มีอยู่แล้ว — รันเพื่อ "เช็คว่าตรง" ไม่ใช่ "ถือว่าตรง"
const CASES = [
  { id: "C1", overpayment: "20000.00", remainingPrincipal: "60000.00",
    note: "กรณีปกติ — ตัวเลขเดียวกับ EX-miniloan-111 (ส่วนเกิน 20,000.00) เงินต้นคงเหลือ 60,000.00 เป็นค่าสมมติที่ตั้งไว้ให้ห่างจากขอบปฏิเสธ (EX-111 เองไม่ได้ระบุเงินต้นคงเหลือ)" },
  { id: "C2", overpayment: "0.01", remainingPrincipal: "60000.00",
    note: "boundary — ตัวเลขเดียวกับ EX-miniloan-112 (ส่วนเกิน 0.01 บาท) → ค่าธรรมเนียม 1% ของ 0.01 = 0.0001 ปัดเป็น 0.00" },
  { id: "C3", overpayment: "5050.51", remainingPrincipal: "5000.00",
    note: "boundary — ตัวเลขเดียวกับ EX-miniloan-148 ทุกตัว: เงินต้นที่ตัดได้ต้องเท่ากับเงินต้นคงเหลือพอดี (0.00 เหลือ) → รับ" },
  { id: "C4", overpayment: "5050.52", remainingPrincipal: "5000.00",
    note: "boundary — ตัวเลขเดียวกับ EX-miniloan-149 ทุกตัว: มากกว่า C3 หนึ่งสตางค์ → เงินต้นที่ตัดได้ต้องเกินเงินต้นคงเหลือ → ปฏิเสธทั้งรายการ" },
  { id: "C5", overpayment: "12345.67", remainingPrincipal: "200000.00",
    note: "ทดสอบการปัด HALF_UP จริงกับเลขไม่กลม — 12345.67 × 1% = 123.4567 ไม่ลงตัวพอดีที่สตางค์" },
];

function runCase(c) {
  return { case: c, result: computePaydown(c) };
}

function summarise(run) {
  const { case: c, result: R } = run;
  const input = { overpayment: c.overpayment, remaining_principal: c.remainingPrincipal };
  if (R.declined) {
    return { input, expected: { computed: false, declined_by: "CALC-miniloan-005@v1 boundary_behavior", reason: R.reason }, note: c.note };
  }
  return {
    input,
    expected: {
      computed: true,
      paydown_fee: baht(R.feeSatang),
      principal_reduction: baht(R.reductionSatang),
      new_remaining_principal: baht(R.newRemainingSatang),
    },
    note: c.note,
  };
}

// ── entry ───────────────────────────────────────────────────────────────────
const argv = process.argv.slice(2);

if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map((c) => summarise(runCase(c))), null, 2));
  process.exit(0);
}

console.log(`CALC-miniloan-005@v1 · ค่าธรรมเนียมการโปะ (BR-miniloan-050@v2) · rounding = HALF_UP\n`);
for (const c of CASES) {
  const run = runCase(c);
  console.log("─".repeat(78));
  console.log(`${c.id} · ส่วนเกิน = ${c.overpayment} บาท · เงินต้นคงเหลือ = ${c.remainingPrincipal} บาท`);
  console.log(`     ${c.note}`);
  if (run.result.declined) {
    console.log(`     → ปฏิเสธ · ${run.result.reason}`);
    continue;
  }
  const R = run.result;
  console.log(`     ค่าธรรมเนียมการโปะ      ${baht(R.feeSatang).padStart(14)}`);
  console.log(`     เงินต้นที่ตัดได้         ${baht(R.reductionSatang).padStart(14)}`);
  console.log(`     ─────────────────────────────────────`);
  console.log(`     เงินต้นคงเหลือใหม่       ${baht(R.newRemainingSatang).padStart(14)}`);
  console.log("");
}

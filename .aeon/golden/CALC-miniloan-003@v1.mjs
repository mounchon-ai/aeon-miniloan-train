/**
 * CALC-miniloan-003@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-003@v1 (วงเงินอนุมัติสูงสุด = MIN(5×รายได้ต่อเดือน, 1,000,000 บาท))
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → เลขทุกตัวเป็น BigInt สตางค์ (จำนวนเต็ม) ไม่มี Number/float แตะเงินเลย
 *   rounding_mode    HALF_UP   → ระบุไว้เพื่อความสอดคล้อง แต่ไม่ถูกเรียกใช้จริงในสัญญานี้
 *   rounding_points  ไม่มีจุดที่ต้องปัดจริง — ตัวคูณ (5) เป็นจำนวนเต็ม เงิน money(2) คูณด้วยจำนวนเต็ม
 *                    ยังคงมี 2 ตำแหน่งทศนิยมพอดีเสมอ ไม่มีเศษเกิดขึ้น · เพดาน 1,000,000.00 ก็มี 2 ตำแหน่งอยู่แล้ว
 *                    สคริปต์นี้จึงทำงานล้วนๆ ด้วยจำนวนเต็มสตางค์ ไม่มี fixed-point/BigInt-scale เข้ามาเกี่ยว
 *   residual_policy  ไม่มี — ไม่ใช่ตารางผ่อน ไม่มีแนวคิดเศษเหลือ
 *   boundary         199,999 → 999,995 (สูตรชนะ) · 200,000 → 1,000,000 พอดี (จุดตรงกัน)
 *                    250,000 → 1,000,000 (เพดานชนะ) · รายได้ < 15,000 → ไม่ถึงสัญญานี้ (ด่านของ BR-miniloan-001@v1)
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-003@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-003@v1.mjs" --json
 */

const CAP_SATANG = 100_000_000n; // 1,000,000.00 บาท
const MULTIPLIER = 5n;

/** เงินบาท (สตริง "199,999.00" หรือ "199999") → สตางค์ (BigInt) แบบตรงเป๊ะ ไม่ผ่าน float */
function moneyToSatang(str) {
  const clean = str.replace(/,/g, "");
  const neg = clean.trim().startsWith("-");
  const [i, f = ""] = clean.trim().replace(/^-/, "").split(".");
  const frac = (f + "00").slice(0, 2);
  const v = BigInt(i || "0") * 100n + BigInt(frac || "0");
  return neg ? -v : v;
}

function computeMax(monthlyIncomeStr) {
  const incomeSatang = moneyToSatang(monthlyIncomeStr);
  const formulaSatang = incomeSatang * MULTIPLIER; // จำนวนเต็ม × จำนวนเต็ม — ไม่มีเศษ ไม่มีจุดปัด
  const capped = formulaSatang > CAP_SATANG;
  const resultSatang = capped ? CAP_SATANG : formulaSatang;
  return { incomeSatang, formulaSatang, capped, resultSatang };
}

// ── กรณีทดสอบ — เลือกตามลำดับของ /req:golden ขั้นที่ 4 ──────────────────────
// ไม่มี source ชนิด sample_data ในสเปกนี้ จึงไม่มีแถวของลูกค้าให้ใส่
const CASES = [
  { id: "C1", note: "กรณีปกติ — ห้าเท่ายังต่ำกว่าเพดานมาก (คล้าย EX-miniloan-019 ในรูปแบบ)", monthlyIncome: "30000.00" },
  { id: "C2", note: "boundary — ค่าสุดท้ายก่อนเพดานเริ่มมีผล (EX-miniloan-020)", monthlyIncome: "199999.00" },
  { id: "C3", note: "boundary — จุดที่สูตรกับเพดานให้คำตอบตรงกันพอดี (EX-miniloan-021)", monthlyIncome: "200000.00" },
  { id: "C4", note: "boundary — เพดานเป็นตัวจำกัด ไม่ใช่สูตร (EX-miniloan-022)", monthlyIncome: "250000.00" },
  { id: "C5", note: "boundary รายได้ขั้นต่ำของ BR-miniloan-001@v1 (15,000 พอดี)", monthlyIncome: "15000.00" },
  { id: "C6", note: "พิสูจน์ว่าไม่มีจุดปัดจริง — รายได้มีสตางค์ (.33) คูณ 5 แล้วยังคง 2 ตำแหน่งพอดี ไม่มีเศษ", monthlyIncome: "45678.33" },
];

// ── การแสดงผล ───────────────────────────────────────────────────────────────
const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};

function summarise(c) {
  const R = computeMax(c.monthlyIncome);
  return {
    input: { monthly_income: c.monthlyIncome },
    expected: {
      formula_value: baht(R.formulaSatang),
      cap: "1,000,000.00",
      limited_by: R.capped ? "cap" : "formula",
      max_approvable: baht(R.resultSatang),
    },
    note: c.note,
  };
}

// ── entry ───────────────────────────────────────────────────────────────────
const argv = process.argv.slice(2);

if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map(summarise), null, 2));
  process.exit(0);
}

console.log("CALC-miniloan-003@v1 · วงเงินอนุมัติสูงสุด = MIN(5×รายได้, 1,000,000) · ไม่มีจุดปัด (จำนวนเต็ม×เงิน)\n");
for (const c of CASES) {
  const s = summarise(c);
  console.log("─".repeat(78));
  console.log(`${c.id} · รายได้ต่อเดือน ${s.input.monthly_income}`);
  console.log(`     ${c.note}`);
  console.log(`     5×รายได้ = ${s.expected.formula_value} · เพดาน = ${s.expected.cap} · จำกัดโดย: ${s.expected.limited_by}`);
  console.log(`     → วงเงินอนุมัติสูงสุด = ${s.expected.max_approvable}`);
  console.log("");
}

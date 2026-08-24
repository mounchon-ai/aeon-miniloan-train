/**
 * CALC-miniloan-005@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-003@v1 (วงเงินอนุมัติสูงสุด)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   formula          MaxApprovableAmount = MIN(5 × monthly_income, 1,000,000)
 *                    ค่าคงที่ 5 และ 1,000,000 ฝังในสูตรของสัญญาเอง ไม่ใช่ input
 *   inputs           monthly_income: money(2) เท่านั้น
 *   numeric_type     decimal   → BigInt satang ล้วน ไม่มี Number/float แตะเงินเลย
 *   rounding_mode    HALF_UP   → ระบุไว้ตามธรรมเนียมระบบ (BR-miniloan-035@v1) แต่เป็น field เฉย (inert)
 *   rounding_points  ไม่มีจุดปัดที่เกิดขึ้นจริง — 5×satang ยังเป็นจำนวนเต็มสตางค์ และ MIN() กับค่าคงที่
 *                    จำนวนเต็มบาทไม่แนะนำทศนิยมใหม่ — สคริปต์นี้จึงไม่มีฟังก์ชันปัดเลย
 *   residual_policy  ไม่มี — สูตรนี้ไม่มีการแบ่งงวด
 *   boundary         monthly_income × 5 < 1,000,000 → ใช้สูตร 5× ตรงๆ
 *                    monthly_income × 5 = 1,000,000 พอดี → สองทางให้คำตอบเดียวกัน
 *                    monthly_income × 5 > 1,000,000 → ถูกจำกัดที่เพดาน
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-005@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-005@v1.mjs" --json
 */

const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};

const CAP_SATANG = 1_000_000n * 100n; // 1,000,000.00 บาท

// ── สัญญาการคำนวณ ───────────────────────────────────────────────────────────
function computeMax({ monthlyIncomeBaht }) {
  const incomeSatang = BigInt(Math.round(monthlyIncomeBaht * 100));
  const fiveTimesSatang = 5n * incomeSatang; // จำนวนเต็ม × จำนวนเต็ม = จำนวนเต็ม ไม่มีเศษ
  const maxSatang = fiveTimesSatang < CAP_SATANG ? fiveTimesSatang : CAP_SATANG;
  const branch = fiveTimesSatang < CAP_SATANG ? "5×รายได้ (ต่ำกว่าเพดาน)"
    : fiveTimesSatang === CAP_SATANG ? "5×รายได้ = เพดานพอดี (สองสูตรตรงกัน)"
    : "เพดาน 1,000,000 (5×รายได้เกินเพดาน)";
  return { incomeSatang, fiveTimesSatang, maxSatang, branch, cappedBy: fiveTimesSatang >= CAP_SATANG ? "cap" : "formula" };
}

// ── กรณีทดสอบ ────────────────────────────────────────────────────────────────
const CASES = [
  { id: "C1", kind: "happy", monthlyIncomeBaht: 30_000,
    note: "= EX-miniloan-019 — ห่างจากเพดานชัดเจน" },
  { id: "C2", kind: "boundary", monthlyIncomeBaht: 199_999,
    note: "= EX-miniloan-020 — ค่าสุดท้ายก่อนเพดานมีผล (5×199,999 = 999,995 ยังต่ำกว่า 1,000,000 อยู่ 5 บาท)" },
  { id: "C3", kind: "boundary", monthlyIncomeBaht: 200_000,
    note: "= EX-miniloan-021 — รอยต่อที่สูตรกับเพดานให้คำตอบตรงกันพอดี (5×200,000 = 1,000,000 เป๊ะ)" },
  { id: "C4", kind: "boundary", monthlyIncomeBaht: 250_000,
    note: "= EX-miniloan-022 — ถูกจำกัดด้วยเพดาน ไม่ใช่ 5 เท่าของรายได้ (5×250,000 = 1,250,000 > เพดาน)" },
  { id: "C5", kind: "boundary", monthlyIncomeBaht: 199_999.99,
    note: "ขอบที่แน่นกว่า C2/C3 ที่ระดับสตางค์ — 5×199,999.99 = 999,999.95 ต่ำกว่าเพดานอยู่แค่ 5 สตางค์ (0.05 บาท) ไม่มีใบตัวอย่างคู่ พิสูจน์ผ่าน golden dataset เท่านั้น" },
];

function summarise(c) {
  const R = computeMax(c);
  return {
    input: { monthly_income: baht(R.incomeSatang) },
    expected: {
      five_times_income: baht(R.fiveTimesSatang),
      cap: baht(CAP_SATANG),
      max_approvable_amount: baht(R.maxSatang),
      branch: R.branch,
      capped_by: R.cappedBy,
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

console.log("CALC-miniloan-005@v1 · วงเงินอนุมัติสูงสุด = MIN(5×รายได้, 1,000,000) · ไม่มีจุดปัดที่เกิดขึ้นจริง\n");
for (const c of CASES) {
  const R = computeMax(c);
  console.log("─".repeat(90));
  console.log(`${c.id} [${c.kind}] · monthly_income = ${baht(R.incomeSatang)}`);
  console.log(`     ${c.note}`);
  console.log(`     5×รายได้ = ${baht(R.fiveTimesSatang)} · เพดาน = ${baht(CAP_SATANG)} · สาขา: ${R.branch}`);
  console.log(`     → วงเงินอนุมัติสูงสุด = ${baht(R.maxSatang)}`);
  console.log("");
}

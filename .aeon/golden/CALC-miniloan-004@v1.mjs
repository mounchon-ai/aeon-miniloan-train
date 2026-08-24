/**
 * CALC-miniloan-004@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-002@v1 (Debt-to-Income ≤ 70%)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → BigInt fixed-point / เศษส่วนตรง ไม่มี Number/float แตะเงินหรือ DTI เลย
 *   rounding_mode    HALF_UP   → divRoundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ
 *   formula          DTI (แสดงผล) = (existing_monthly_debt + new_installment) / monthly_income
 *                    เกณฑ์ผ่าน/ตกจริง = (existing_monthly_debt + new_installment) ≤ 0.70 × monthly_income
 *                    เทียบกันเป็นบาทที่ความละเอียดเต็ม — ไม่ใช่เทียบที่ตัว % ที่ปัดแล้ว
 *   inputs           new_installment มาจาก CALC-miniloan-001@v2 ตรงๆ (import computeSchedule มาเรียก
 *                    ไม่คำนวณ EMI ซ้ำในไฟล์นี้ — ตามที่สัญญาห้ามไว้ และตามที่ EX-miniloan-153 พิสูจน์)
 *   rounding_points  new_installment รับมาปัดแล้วเป็น money(2) จาก CALC-miniloan-001@v2 (ไม่ปัดซ้ำ) ·
 *                    เกณฑ์ผ่าน/ตกเทียบที่ความละเอียดเต็ม ไม่ปัดก่อนเทียบ · DTI% ที่แสดงผล =
 *                    round((existing+new)/income × 100, 2) HALF_UP — เป็นค่าที่ใช้ "แสดง" เท่านั้น
 *                    ไม่ป้อนกลับเข้าไปตัดสินผ่าน/ตก
 *   residual_policy  ไม่มี — DTI เป็นอัตราส่วน ไม่ใช่ตารางผ่อน
 *   boundary         monthly_income < 15,000 บาท → ถูกปฏิเสธตั้งแต่ BR-miniloan-001@v1 ก่อนถึงสัญญานี้
 *                    ผลรวม = 0.70 × รายได้ พอดี → ผ่าน ไม่ใช่ตก
 *                    วงเงินถูกปรับลงภายหลัง → ไม่คำนวณ DTI ใหม่ (ไม่ใช่กรณีตัวเลข — ไม่มีแถวในไฟล์นี้)
 *
 * ข้อที่สัญญาไม่ได้พิน และสคริปต์ต้องเลือกเอง — ประกาศไว้ตรงนี้:
 *   เกณฑ์ผ่าน/ตก "เทียบที่ความละเอียดเต็ม" ไม่ได้บอกว่าเทียบด้วยวิธีไหนในโค้ด — 0.70 × รายได้ อาจไม่ลง
 *   ตัวเป็นจำนวนสตางค์เต็ม (เช่น รายได้ 30,000.01 บาท) สคริปต์นี้เลี่ยง float/การปัดใดๆ ทั้งสิ้นโดยคูณ
 *   ไขว้แทนหาร: ผ่าน ⟺ 10 × (existing_satang + new_satang) ≤ 7 × income_satang — เป็นอสมการจำนวนเต็ม
 *   ล้วนๆ ตรงตามนิยาม 0.70 = 7/10 เป๊ะ ไม่มีการปัดหรือประมาณค่าใดๆ แทรกเข้ามาเลย
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-004@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-004@v1.mjs" --json
 */

import { computeSchedule } from "./CALC-miniloan-001@v2.mjs";

const EMI_SCALE = 30; // เดียวกับค่าเริ่มต้นของ CALC-miniloan-001@v2.mjs เพื่อให้ new_installment ตรงกันเป๊ะ

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

const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};

/** hundredths ของ % (7000 = 70.00%) → "70.00" */
const pct = (hundredths) => {
  const neg = hundredths < 0n;
  const a = neg ? -hundredths : hundredths;
  return (neg ? "-" : "") + (a / 100n).toString() + "." + (a % 100n).toString().padStart(2, "0");
};

// ── ด่าน BR-miniloan-001@v1 (ไม่ใช่ของสัญญา — สัญญาแค่บอกว่าตัวเองไม่ถูกเรียก) ──
function acceptedByBR001({ monthlyIncomeBaht }) {
  if (monthlyIncomeBaht < 15_000)
    return { ok: false, reason: `รายได้ ${monthlyIncomeBaht} บาท/เดือน ต่ำกว่าขั้นต่ำ 15,000 บาท ที่ BR-miniloan-001@v1 กำหนด — ถูกปฏิเสธก่อนถึงสัญญานี้` };
  return { ok: true };
}

// ── สัญญาการคำนวณ ───────────────────────────────────────────────────────────
function computeDti({ existingMonthlyDebtBaht, monthlyIncomeBaht, loan, newInstalmentOverrideSatang }) {
  const existingSatang = BigInt(Math.round(existingMonthlyDebtBaht * 100));
  const incomeSatang = BigInt(Math.round(monthlyIncomeBaht * 100));

  // new_installment: เรียก CALC-miniloan-001@v2 ตรงๆ — ไม่คำนวณ EMI ซ้ำที่นี่
  // ยกเว้นกรณีที่ตั้งใจ override (C5) เพื่อแยกทดสอบการปัด % แสดงผลล้วนๆ โดยไม่ให้ EMI จริงเจือปน —
  // ไม่ใช่พฤติกรรมของสัญญา แค่เทคนิคแยกตัวแปรของกรณีทดสอบข้อนี้ข้อเดียว
  let newInstalmentSatang, branch;
  if (newInstalmentOverrideSatang !== undefined) {
    newInstalmentSatang = newInstalmentOverrideSatang;
    branch = "override เพื่อแยกทดสอบการปัด %";
  } else {
    const schedule = computeSchedule(loan, EMI_SCALE);
    newInstalmentSatang = schedule.instalmentSatang;
    branch = schedule.branch;
  }

  const totalSatang = existingSatang + newInstalmentSatang;

  // เกณฑ์ผ่าน/ตก — อสมการจำนวนเต็มล้วน 10×total ≤ 7×income (= total ≤ 0.70×income แบบไม่ปัด)
  const passes = 10n * totalSatang <= 7n * incomeSatang;

  // DTI% สำหรับแสดงผล — ปัดแยกต่างหาก ไม่ป้อนกลับเข้าไปตัดสิน
  const dtiHundredths = divRoundHalfUp(totalSatang * 10000n, incomeSatang);

  return { existingSatang, incomeSatang, newInstalmentSatang, totalSatang, passes, dtiHundredths, branch };
}

// ── กรณีทดสอบ ────────────────────────────────────────────────────────────────
// C2/C3/C4 อนุมาน existing_monthly_debt จาก new_installment ที่คำนวณจริง (ไม่ใช่เลขที่เดามาก่อน)
// เพื่อให้ผลรวมตกที่ขอบพอดี — ใช้ helper คำนวณตอนรัน ไม่ใช่ค่าคงที่ที่คิดในหัว
const BOUNDARY_LOAN = { principalBaht: 150_000, annualRate: "0.25", termMonths: 24 };
const BOUNDARY_INCOME_BAHT = 30_000; // เดียวกับ EX-miniloan-037/038 — 70% ของ 30,000 = 21,000 พอดี ลงตัวเป็นสตางค์

function boundaryExistingDebtBaht(offsetSatang) {
  const incomeSatang = BigInt(BOUNDARY_INCOME_BAHT * 100);
  const thresholdSatang = (incomeSatang * 7n) / 10n; // 30,000 หาร 10 ลงตัว → ไม่มีเศษ ไม่ต้องปัด
  const newInstalmentSatang = computeSchedule(BOUNDARY_LOAN, EMI_SCALE).instalmentSatang;
  const existingSatang = thresholdSatang - newInstalmentSatang + offsetSatang;
  return Number(existingSatang) / 100;
}

const CASES = [
  {
    id: "C1", kind: "happy",
    existingMonthlyDebtBaht: 5_000, monthlyIncomeBaht: 30_000,
    loan: { principalBaht: 100_000, annualRate: "0.25", termMonths: 12 },
    note: "ห่างจากเพดานชัดเจน — เดียวกับสไตล์ EX-miniloan-039 (ภาระหนี้เดิม 5,000 + งวดใหม่จากเงินกู้ 100,000/12 งวด)",
  },
  {
    id: "C2", kind: "boundary",
    existingMonthlyDebtBaht: boundaryExistingDebtBaht(0n), monthlyIncomeBaht: BOUNDARY_INCOME_BAHT,
    loan: BOUNDARY_LOAN,
    note: "ผลรวม = 0.70 × รายได้ พอดี (offset = 0 สตางค์) → ต้องผ่าน — พิสูจน์ EX-miniloan-037 ด้วยตัวเลขจริง",
  },
  {
    id: "C3", kind: "exception",
    existingMonthlyDebtBaht: boundaryExistingDebtBaht(100n), monthlyIncomeBaht: BOUNDARY_INCOME_BAHT,
    loan: BOUNDARY_LOAN,
    note: "เกินเพดานอยู่ 1.00 บาท (offset = +100 สตางค์) → ต้องตก — พิสูจน์ EX-miniloan-038 ด้วยตัวเลขจริง: DTI% ที่ปัดแสดงจะเท่ากับ C2 เป๊ะ (70.00%) แต่ผลต้องต่างกัน",
  },
  {
    id: "C4", kind: "boundary",
    existingMonthlyDebtBaht: boundaryExistingDebtBaht(1n), monthlyIncomeBaht: BOUNDARY_INCOME_BAHT,
    loan: BOUNDARY_LOAN,
    note: "เกินเพดานแค่ 1 สตางค์ (offset = +1 สตางค์) → ต้องตก — ขอบที่แน่นกว่า C3 พิสูจน์ว่าเทียบกันที่สตางค์จริง ไม่ใช่ที่บาทหรือ % ที่ปัดแล้ว",
  },
  {
    id: "C5", kind: "boundary",
    existingMonthlyDebtBaht: 4_000.80, monthlyIncomeBaht: 16_000,
    newInstalmentOverrideSatang: 0n, // ตั้งเป็น 0 ตรงๆ เพื่อแยกทดสอบการปัด % แสดงผลล้วนๆ ไม่ให้ EMI จริงเจือปน (ดูหมายเหตุที่หัวไฟล์)
    note: "ผลหาร 4,000.80/16,000 = 25.005% เป๊ะ — จุดที่ HALF_UP กับ HALF_EVEN ให้คำตอบต่างกัน (25.01% vs 25.00%) พิสูจน์ว่า rounding_mode ที่เลือกไว้ (HALF_UP) มีผลจริงที่ตัวเลขแสดงผล",
  },
  {
    id: "C6", kind: "exception", expectDeclined: true,
    monthlyIncomeBaht: 14_999,
    note: "รายได้ต่ำกว่าขั้นต่ำอยู่ 1 บาท — ถูกปฏิเสธตั้งแต่ BR-miniloan-001@v1 สัญญานี้ไม่ถูกเรียก",
  },
  {
    id: "C7", kind: "alternate",
    existingMonthlyDebtBaht: 5_000, monthlyIncomeBaht: 30_000,
    loan: { principalBaht: 240_000, annualRate: "0.25", termMonths: 24 },
    note: "เดียวกับ EX-miniloan-153 (เงินกู้ 240,000/24 งวด) — ยืนยันว่า new_installment ที่ DTI ใช้ตรงกับค่างวดที่หน้าตัวอย่างตารางผ่อนแสดงเป๊ะ เพราะเรียกฟังก์ชันเดียวกัน",
  },
];

function runCase(c) {
  if (c.expectDeclined) return { case: c, declined: true, gate: acceptedByBR001({ monthlyIncomeBaht: c.monthlyIncomeBaht }) };
  const gate = acceptedByBR001({ monthlyIncomeBaht: c.monthlyIncomeBaht });
  const R = computeDti(c);
  return { case: c, declined: false, gate, result: R };
}

function summarise(run) {
  const { case: c } = run;
  if (run.declined) {
    return {
      input: { existing_monthly_debt: c.existingMonthlyDebtBaht ?? null, monthly_income: c.monthlyIncomeBaht, loan: c.loan ?? null },
      expected: { computed: false, declined_by: "BR-miniloan-001@v1", reason: run.gate.reason },
      note: c.note,
    };
  }
  const R = run.result;
  return {
    input: {
      existing_monthly_debt: baht(R.existingSatang),
      monthly_income: baht(R.incomeSatang),
      loan: c.loan,
    },
    expected: {
      computed: true,
      new_installment: baht(R.newInstalmentSatang),
      emi_branch: R.branch,
      total: baht(R.totalSatang),
      threshold_070_income: baht((R.incomeSatang * 7n) / 10n),
      dti_display: pct(R.dtiHundredths) + "%",
      passes: R.passes,
      band_input: R.passes ? "ผ่านเกณฑ์ DTI" : "ไม่ผ่านเกณฑ์ DTI → BR-miniloan-006@v1 ตัดสิน Band C",
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

console.log(`CALC-miniloan-004@v1 · DTI ≤ 70% · rounding = HALF_UP (เฉพาะ % แสดงผล) · เกณฑ์ผ่าน/ตกเทียบที่สตางค์เต็มความละเอียด\n`);
for (const c of CASES) {
  const run = runCase(c);
  console.log("─".repeat(90));
  console.log(`${c.id} [${c.kind}]`);
  console.log(`     ${c.note}`);
  if (run.declined) {
    console.log(`     → ไม่คำนวณ · ${run.gate.reason}`);
    continue;
  }
  const R = run.result;
  const loanDesc = c.loan ? `loan = P ${c.loan.principalBaht.toLocaleString("en-US")} / ${c.loan.annualRate} / n=${c.loan.termMonths}` : "loan = (override — ดูหมายเหตุ)";
  console.log(`     existing_monthly_debt = ${baht(R.existingSatang)} · monthly_income = ${baht(R.incomeSatang)} · ${loanDesc}`);
  console.log(`     new_installment (${R.branch}) = ${baht(R.newInstalmentSatang)}`);
  console.log(`     total = ${baht(R.totalSatang)} · เกณฑ์ 0.70×income = ${baht((R.incomeSatang * 7n) / 10n)} · DTI แสดงผล = ${pct(R.dtiHundredths)}%`);
  console.log(`     → ${R.passes ? "✅ ผ่าน" : "❌ ตก"}`);
  console.log("");
}

/**
 * CALC-miniloan-002@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-002@v1 (Debt-to-Income ≤ 70%)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → เลขทุกตัวเป็น BigInt fixed-point ไม่มี Number/float ในการคำนวณเงินเลย
 *   rounding_mode    HALF_UP   → roundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ
 *   rounding_points  งวดใหม่โดยประมาณ = round(EMI,2) จาก CALC-miniloan-001@v2 (ไม่ใช่ EMI ดิบ)
 *                    — ใช้สูตร EMI เดียวกันเป๊ะ (verbatim จาก golden/CALC-miniloan-001@v2.mjs)
 *                    เปรียบเทียบผ่าน/ไม่ผ่าน 70% ด้วยยอดบาทที่แน่นอน ไม่ใช่เปอร์เซ็นต์ที่ปัดแล้ว:
 *                    PASS  ⟺  (existing_monthly_debt + งวดใหม่) × 10  ≤  monthly_income × 7   (สตางค์ทั้งคู่)
 *                    เทียบด้วยการคูณไขว้แบบ BigInt ล้วน ไม่มีการหารหรือปัดใดๆ ก่อนตัดสิน —
 *                    เพราะ monthly_income × 0.70 ไม่ลงตัวเป็นสตางค์เสมอไป (เช่น 15,000.33 บาท)
 *                    เปอร์เซ็นต์ที่แสดงหน้าจอ (เช่น "70.00%") เป็นค่าที่ปัด 2 ตำแหน่งไว้แสดงผลเท่านั้น
 *                    — ไม่ใช่ค่าที่ใช้ตัดสิน (ตาม EX-miniloan-038 ที่ยืนยันไว้แล้วในสเปก)
 *   residual_policy  ไม่มี — การเช็คเกณฑ์ผ่าน/ไม่ผ่านไม่มีแนวคิดเศษเหลือ
 *   boundary         DTI = 70.00% พอดี → ผ่าน (ไม่ใช่ตก)
 *                    รายได้ / เงินกู้ / จำนวนงวดนอกช่วง BR-001 / BR-004 → ไม่ถึงสัญญานี้ (ด่านของกฎอื่น)
 *                    annual_rate = 0% → งวดใหม่ = P/n ตาม boundary ของ CALC-miniloan-001@v2 เอง
 *
 * ข้อสังเกต: EX-miniloan-037/038/039/040 ที่มีอยู่แล้วในสเปกใช้ตัวเลข "ภาระหนี้รวม" แบบลอย
 * ไม่ผูกกับเงินกู้/งวด/อัตราที่ระบุ (เพราะเขียนไว้ก่อนมีสัญญาการคำนวณ) — ชุดทดสอบด้านล่างจึงคิด
 * อินพุตที่ครบทั้งห้าช่องขึ้นใหม่ให้ตรงกับ **รูปแบบขอบ** เดียวกัน (70% พอดี / เกิน 1 สตางค์ / rate=0)
 * ไม่ใช่การก็อปตัวเลขจากใบตัวอย่างเหล่านั้นตรงๆ
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-002@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-002@v1.mjs" --json
 */

// ── fixed-point decimal บน BigInt (เหมือน CALC-miniloan-001@v2.mjs ทุกประการ) ────
const pow10 = (k) => 10n ** BigInt(k);

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
    fromDecimalString(str) {
      const neg = str.trim().startsWith("-");
      const [i, f = ""] = str.trim().replace(/^-/, "").split(".");
      const frac = (f + "0".repeat(scale)).slice(0, scale);
      const v = BigInt(i || "0") * ONE + BigInt(frac || "0");
      return neg ? -v : v;
    },
    mul: (a, b) => divRoundHalfUp(a * b, ONE),
    div: (a, b) => divRoundHalfUp(a * ONE, b),
    toSatang: (a) => divRoundHalfUp(a * 100n, ONE),
  };
};

/** เงินบาท (สตริง เช่น "11,495.58" หรือ "11495.58") → สตางค์ (BigInt) แบบตรงเป๊ะ ไม่ผ่าน float */
function moneyToSatang(str) {
  const clean = str.replace(/,/g, "");
  const neg = clean.trim().startsWith("-");
  const [i, f = ""] = clean.trim().replace(/^-/, "").split(".");
  const frac = (f + "00").slice(0, 2);
  const v = BigInt(i || "0") * 100n + BigInt(frac || "0");
  return neg ? -v : v;
}

// ── EMI ตาม CALC-miniloan-001@v2 — verbatim (คัดลอกเฉพาะค่างวดระดับเดียว ไม่ต้องเดินตาราง) ──
function instalmentSatangFor({ principalBaht, annualRate, termMonths }, scale) {
  const fx = makeFx(scale);
  const n = termMonths;
  const P = fx.fromInt(principalBaht);
  const r = fx.div(fx.fromDecimalString(annualRate), fx.fromInt(12));

  let emiRaw;
  if (r === 0n) {
    emiRaw = fx.div(P, fx.fromInt(n));
  } else {
    let pow = fx.ONE;
    const onePlusR = fx.ONE + r;
    for (let k = 0; k < n; k++) pow = fx.mul(pow, onePlusR);
    emiRaw = fx.div(fx.mul(fx.mul(P, r), pow), pow - fx.ONE);
  }
  return fx.toSatang(emiRaw);
}

// ── สัญญาการคำนวณ CALC-miniloan-002@v1 ───────────────────────────────────────
function evaluateDTI(c, scale) {
  const newInstalmentSatang = instalmentSatangFor(
    { principalBaht: c.requestedPrincipal, annualRate: c.annualRate, termMonths: c.requestedTermMonths },
    scale,
  );
  const existingDebtSatang = moneyToSatang(c.existingMonthlyDebt);
  const incomeSatang = moneyToSatang(c.monthlyIncome);
  const totalDebtSatang = existingDebtSatang + newInstalmentSatang;

  // ตัดสินผ่าน/ไม่ผ่านด้วยการคูณไขว้ BigInt ล้วน — ไม่มีการหารหรือปัดใดๆ ก่อนตัดสิน
  const pass = totalDebtSatang * 10n <= incomeSatang * 7n;

  // เปอร์เซ็นต์ที่แสดงผล — ปัด 2 ตำแหน่ง HALF_UP เพื่อการแสดงผลเท่านั้น ไม่ใช่ตัวตัดสิน
  const dtiPercentX100 = divRoundHalfUp(totalDebtSatang * 10000n, incomeSatang);

  return { newInstalmentSatang, existingDebtSatang, incomeSatang, totalDebtSatang, pass, dtiPercentX100 };
}

// ── กรณีทดสอบ — เลือกตามลำดับของ /req:golden ขั้นที่ 4 ──────────────────────
// ไม่มี source ชนิด sample_data ในสเปกนี้ จึงไม่มีแถวของลูกค้าให้ใส่
const CASES = [
  {
    id: "C1", note: "กรณีปกติ — ห่างจากเพดานชัดเจน (คล้าย EX-miniloan-039 ในรูปแบบ ไม่ใช่ตัวเลขเดียวกัน)",
    existingMonthlyDebt: "10500.00", monthlyIncome: "30000.00",
    requestedPrincipal: 100000, annualRate: "0.25", requestedTermMonths: 12,
  },
  {
    id: "C2", note: "boundary — DTI = 70.00% พอดี ต้องผ่าน ไม่ใช่ตก (คล้าย EX-miniloan-037 ในรูปแบบ)",
    existingMonthlyDebt: "11495.58", monthlyIncome: "30000.00",
    requestedPrincipal: 100000, annualRate: "0.25", requestedTermMonths: 12,
  },
  {
    id: "C3", note: "exception — เกินเพดานไป 1 สตางค์เป๊ะ · เปอร์เซ็นต์ที่ปัดแล้วยังเป็น 70.00% เท่ากับ C2 (คล้าย EX-miniloan-038 ในรูปแบบ) — พิสูจน์ว่าต้องเทียบยอดบาทจริง ไม่ใช่เปอร์เซ็นต์ที่ปัดแล้ว",
    existingMonthlyDebt: "11495.59", monthlyIncome: "30000.00",
    requestedPrincipal: 100000, annualRate: "0.25", requestedTermMonths: 12,
  },
  {
    id: "C4", note: "boundary annual_rate = 0% — งวดใหม่ = P/n ตาม boundary ของ CALC-miniloan-001@v2 เอง",
    existingMonthlyDebt: "3000.00", monthlyIncome: "20000.00",
    requestedPrincipal: 120000, annualRate: "0", requestedTermMonths: 12,
  },
  {
    id: "C5", note: "กรณีปกติ DTI ต่ำ — เงินกู้/งวด/อัตราขอบล่างของ BR-miniloan-004@v1 (10,000/6/25%)",
    existingMonthlyDebt: "500.00", monthlyIncome: "15000.00",
    requestedPrincipal: 10000, annualRate: "0.25", requestedTermMonths: 6,
  },
];

// ── การแสดงผล ───────────────────────────────────────────────────────────────
const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};
const pct = (x100) => {
  const neg = x100 < 0n;
  const a = neg ? -x100 : x100;
  return (neg ? "-" : "") + (a / 100n).toString() + "." + (a % 100n).toString().padStart(2, "0") + "%";
};

function summarise(c, scale) {
  const R = evaluateDTI(c, scale);
  return {
    input: {
      existing_monthly_debt: c.existingMonthlyDebt,
      monthly_income: c.monthlyIncome,
      requested_principal: c.requestedPrincipal,
      requested_term_months: c.requestedTermMonths,
      annual_rate: c.annualRate,
    },
    expected: {
      new_instalment: baht(R.newInstalmentSatang),
      total_debt: baht(R.totalDebtSatang),
      threshold_ratio: "0.70",
      dti_shown: pct(R.dtiPercentX100),
      pass: R.pass,
    },
    note: c.note,
  };
}

// ── entry ───────────────────────────────────────────────────────────────────
const argv = process.argv.slice(2);
const flag = (name, dflt) => { const i = argv.indexOf(name); return i < 0 ? dflt : argv[i + 1]; };
const WORKING_SCALE = Number(flag("--scale", 30));

if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map((c) => summarise(c, WORKING_SCALE)), null, 2));
  process.exit(0);
}

console.log(`CALC-miniloan-002@v1 · Debt-to-Income ≤ 70% · working scale = ${WORKING_SCALE} · rounding = HALF_UP\n`);
for (const c of CASES) {
  const s = summarise(c, WORKING_SCALE);
  console.log("─".repeat(78));
  console.log(`${c.id} · หนี้เดิม ${s.input.existing_monthly_debt} · รายได้ ${s.input.monthly_income} · เงินกู้ที่ขอ ${s.input.requested_principal.toLocaleString("en-US")} · ${s.input.requested_term_months} งวด · อัตรา ${s.input.annual_rate}`);
  console.log(`     ${c.note}`);
  console.log(`     งวดใหม่ (round(EMI,2)) = ${s.expected.new_instalment} · ภาระหนี้รวม = ${s.expected.total_debt} · เพดาน = ${s.expected.threshold_ratio} ของรายได้`);
  console.log(`     DTI ที่แสดง = ${s.expected.dti_shown} · ผลตัดสิน (จากยอดบาทจริง) = ${s.expected.pass ? "✅ ผ่าน" : "✗ ไม่ผ่าน"}`);
  console.log("");
}

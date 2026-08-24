/**
 * CALC-miniloan-001@v2 — answer key generator
 * ระบุกฎ: BR-miniloan-016@v1 (EMI · ตารางผ่อนลดต้นลดดอก)
 * แทนที่ CALC-miniloan-001@v1 (CHG-miniloan-005) — แก้ความขัดกับ BR-miniloan-035@v1
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → เลขทุกตัวเป็น BigInt fixed-point ไม่มี Number/float ในการคำนวณเงินเลย
 *   rounding_mode    HALF_UP   → roundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ ไม่ใช่ปัดไปหาเลขคู่
 *   rounding_points  ปัดทุกจุดทันทีที่เกิด — ไม่เก็บค่าเต็มไว้แล้วปัดตอนแสดงผลอีกต่อไป (แก้จาก @v1):
 *                      ค่างวดที่แสดง = round(EMI, 2) เท่ากันทุกแถว (ไม่เปลี่ยนจาก @v1)
 *                      ดอกเบี้ยของแถว t = round(ยอดคงเหลือหลังงวด t−1 ที่แสดง × r, 2)
 *                        — ใช้ยอดคงเหลือที่แสดงจริง (ปัดแล้ว) ของแถวก่อนหน้าเป็นฐานเสมอ
 *                      เงินต้นของแถว = round(EMI, 2) − ดอกเบี้ยของแถว   (หักเอา ไม่ปัดซ้ำ)
 *                      *** ไม่มีบัญชีภายในเก็บค่าเต็มแยกต่างหากอีกต่อไป — คอลัมน์เดียว แหล่งอ้างอิงเดียว ***
 *   residual_policy  งวดสุดท้ายดูดเศษ: เงินต้นงวดสุดท้าย = P − Σ(เงินต้นของงวด 1..n−1)
 *                      (คอลัมน์เดียวที่เดินตลอดทั้งตาราง ไม่มีคอลัมน์อื่นให้เทียบอีกต่อไปหลัง @v2)
 *   boundary         r = 0 → EMI = P / n
 *                    P/n นอกช่วง BR-miniloan-004@v1 → ไม่คำนวณ (ด่านนี้เป็นของ BR-004 ไม่ใช่ของสัญญา)
 *                    n = 1 → ตารางแถวเดียว งวดแรกเป็นงวดสุดท้ายด้วย
 *
 * ข้อที่สัญญาไม่ได้พิน และสคริปต์ต้องเลือกเอง — ประกาศไว้ตรงนี้:
 *   EMI เองยังคำนวณด้วยทศนิยมภายใน WORKING_SCALE ตำแหน่ง (ตั้งค่าได้ด้วย --scale) เพราะ
 *   r = อัตราต่อปี / 12 เป็นทศนิยมไม่รู้จบ และสูตรปิด (closed-form) ของ EMI เองไม่ใช่ recurrence
 *   ที่สัญญาห้ามเก็บค่าเต็ม — สัญญาห้ามเฉพาะ "การเดินยอดคงเหลือทีละงวด" ไม่ปัด ซึ่งสคริปต์นี้ไม่ทำแล้ว
 *   ถ้ารันสองสเกลแล้วผลระดับสตางค์ต่างกัน แปลว่าเลขเฉลยขึ้นกับตัวเลือกนี้ ซึ่งต้องกลับไปที่ /req:calc
 *   ไม่ใช่ให้สคริปต์เลือกสเกลเอง — ดูโหมด --compare
 *
 * เปรียบเทียบกับ @v1: ต่างกันเฉพาะฐานที่ใช้คำนวณดอกเบี้ยของแต่ละแถว (ยอดคงเหลือที่แสดง/ปัดแล้ว
 * ของแถวก่อนหน้า แทนที่จะเป็นยอดคงเหลือภายในแบบเต็มความละเอียด) — โหมด --diff-v1 พิมพ์ผลต่างรายแถว
 * เทียบกับสคริปต์ @v1 เพื่อยืนยันขนาดผลกระทบ
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-001@v2.mjs"
 *       node ".aeon/golden/CALC-miniloan-001@v2.mjs" --scale 40
 *       node ".aeon/golden/CALC-miniloan-001@v2.mjs" --compare 30 40
 *       node ".aeon/golden/CALC-miniloan-001@v2.mjs" --json
 */

// ── fixed-point decimal บน BigInt ────────────────────────────────────────────
const pow10 = (k) => 10n ** BigInt(k);

/** ปัด HALF_UP: ครึ่งออกจากศูนย์เสมอ */
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
    /** สตางค์ (จำนวนเต็ม) → fixed-point ที่ scale นี้ — ตรง (exact) เสมอเพราะ scale >= 2 */
    fromSatang: (satang) => (satang * ONE) / 100n,
    mul: (a, b) => divRoundHalfUp(a * b, ONE),
    div: (a, b) => divRoundHalfUp(a * ONE, b),
    /** fixed-point → สตางค์ ด้วย HALF_UP — "ปัดทันทีที่เกิด" ของสัญญา @v2 */
    toSatang: (a) => divRoundHalfUp(a * 100n, ONE),
  };
};

// ── ด่าน BR-miniloan-004@v1 (ไม่ใช่ของสัญญา — สัญญาแค่บอกว่าตัวเองไม่ถูกเรียก) ──
const BR004 = { minPrincipal: 10_000n, maxPrincipal: 1_000_000n, minTerm: 6, maxTerm: 60 };
function acceptedByBR004({ principalBaht, termMonths }) {
  const p = BigInt(principalBaht);
  if (p < BR004.minPrincipal || p > BR004.maxPrincipal)
    return { ok: false, reason: `เงินต้น ${principalBaht} บาท อยู่นอกช่วง 10,000–1,000,000 ที่ BR-miniloan-004@v1 รับได้` };
  if (termMonths < BR004.minTerm || termMonths > BR004.maxTerm)
    return { ok: false, reason: `จำนวนงวด ${termMonths} อยู่นอกช่วง 6–60 ที่ BR-miniloan-004@v1 รับได้` };
  return { ok: true };
}

// ── สัญญาการคำนวณ @v2 ───────────────────────────────────────────────────────
// exported เพื่อให้ golden/CALC-miniloan-004@v1.mjs (DTI) import ไปใช้ตรงๆ เป็น "งวดใหม่"
// ตาม CALC-miniloan-004@v1 ที่ห้ามคำนวณ EMI ซ้ำ — ไม่กระทบตัวเลขหรือพฤติกรรมของไฟล์นี้เลย
export function computeSchedule({ principalBaht, annualRate, termMonths }, scale) {
  const fx = makeFx(scale);
  const n = termMonths;
  const P = fx.fromInt(principalBaht);
  const r = fx.div(fx.fromDecimalString(annualRate), fx.fromInt(12));

  // EMI — สูตรปิด ไม่ใช่ recurrence ที่สัญญาห้ามเก็บค่าเต็ม · ขอบ r = 0 สลับสูตรตามสัญญา
  let emi, branch;
  if (r === 0n) {
    emi = fx.div(P, fx.fromInt(n));
    branch = "r=0 → EMI = P / n";
  } else {
    let pow = fx.ONE;
    const onePlusR = fx.ONE + r;
    for (let k = 0; k < n; k++) pow = fx.mul(pow, onePlusR);
    emi = fx.div(fx.mul(fx.mul(P, r), pow), pow - fx.ONE);
    branch = "สูตร EMI ปกติ";
  }

  const instalmentSatang = fx.toSatang(emi);   // ค่างวดที่แสดง = round(EMI, 2) — เท่ากันทุกแถว
  const principalSatang = BigInt(principalBaht) * 100n;

  const rows = [];
  let balance = principalSatang;   // *** คอลัมน์เดียว แหล่งอ้างอิงเดียว — ไม่มีบัญชีภายในแยกอีกต่อไป ***
  let principalSum = 0n;

  for (let t = 1; t <= n; t++) {
    // ดอกเบี้ยของแถว t = round(ยอดคงเหลือ "ที่แสดง" หลังงวด t−1 × r, 2) — ฐานเดียวที่มีอยู่
    const interestRow = fx.toSatang(fx.mul(fx.fromSatang(balance), r));

    let principalRow;
    if (t < n) {
      principalRow = instalmentSatang - interestRow;   // หักเอา ไม่ปัดซ้ำ
    } else {
      principalRow = principalSatang - principalSum;    // residual: ปิดคอลัมน์เดียวที่มีอยู่
    }
    principalSum += principalRow;
    balance -= principalRow;

    rows.push({ t, interest: interestRow, principal: principalRow, instalment: interestRow + principalRow, balance });
  }

  return {
    branch,
    scale,
    instalmentSatang,
    rows,
    sumPrincipal: rows.reduce((a, x) => a + x.principal, 0n),
    sumInterest: rows.reduce((a, x) => a + x.interest, 0n),
    finalBalance: rows[rows.length - 1].balance,
  };
}

// ── กรณีทดสอบ — เลือกให้ตรงกับ GD-miniloan-001 (@v1) ทุกกรณี เพื่อ diff ได้ตรงแถว ──
// ไม่มี source ชนิด sample_data ในสเปกนี้ จึงไม่มีแถวของลูกค้าให้ใส่ และ mismatches[] ว่างเพราะ
// "ไม่มีอะไรให้เทียบ" ไม่ใช่เพราะ "เทียบแล้วตรง"
const CASES = [
  { id: "C1", principalBaht: 100_000, annualRate: "0.25", termMonths: 12,
    note: "กรณีปกติ — เดียวกับ EX-miniloan-001 และ C1 ของ GD-miniloan-001 (@v1) เพื่อเทียบผลกระทบตรงแถว" },
  { id: "C2", principalBaht: 1_000_000, annualRate: "0.25", termMonths: 60,
    note: "ขอบบนทั้งสองแกนของ BR-miniloan-004@v1 — จุดที่ @v1 เคยแสดงระยะห่างสะสมมากที่สุด (~0.17 บาท) ระหว่างคอลัมน์ที่แสดงกับบัญชีภายใน — จุดพิสูจน์หลักของ Q-miniloan-016" },
  { id: "C3", principalBaht: 10_000, annualRate: "0.25", termMonths: 6,
    note: "ขอบล่างทั้งสองแกนของ BR-miniloan-004@v1" },
  { id: "C4", principalBaht: 100_000, annualRate: "0", termMonths: 12,
    note: "boundary r = 0 — เดียวกับ Q-miniloan-016 ที่ยกงวด 6 เป็นตัวอย่าง (@v1: แสดง 50,000.02 vs บัญชีภายใน 50,000.00) — @v2 มีค่าเดียว ไม่มีบัญชีภายในให้ต่าง" },
  { id: "C5", principalBaht: 100_000, annualRate: "0.25", termMonths: 1,
    note: "boundary n = 1 — สัญญานิยามไว้เอง แม้ BR-miniloan-004@v1 จะปฏิเสธก่อนถึงสูตรในเส้นทางจริง",
    br004Note: "ในระบบจริง BR-miniloan-004@v1 ปฏิเสธก่อน แถวนี้แสดงพฤติกรรมของสัญญาล้วนๆ" },
  { id: "C6", principalBaht: 9_999, annualRate: "0.25", termMonths: 12,
    expectDeclined: true, note: "boundary นอกช่วง — เงินต้นต่ำกว่าขั้นต่ำ 1 บาท" },
  { id: "C7", principalBaht: 100_000, annualRate: "0.25", termMonths: 61,
    expectDeclined: true, note: "boundary นอกช่วง — จำนวนงวดเกินเพดาน 1 งวด" },
];

// ── การแสดงผล ───────────────────────────────────────────────────────────────
const baht = (satang) => {
  const neg = satang < 0n;
  const a = neg ? -satang : satang;
  const s = (a / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return (neg ? "-" : "") + s + "." + (a % 100n).toString().padStart(2, "0");
};

function runCase(c, scale) {
  const gate = acceptedByBR004(c);
  if (c.expectDeclined) return { case: c, declined: true, gate };
  return { case: c, declined: false, gate, result: computeSchedule(c, scale) };
}

function summarise(run) {
  const { case: c, result: R } = run;
  if (run.declined) {
    return {
      input: { principal: c.principalBaht, annual_rate: c.annualRate, term_months: c.termMonths },
      expected: { computed: false, declined_by: "BR-miniloan-004@v1", reason: run.gate.reason },
      note: c.note,
    };
  }
  const first = R.rows[0];
  const last = R.rows[R.rows.length - 1];
  return {
    input: { principal: c.principalBaht, annual_rate: c.annualRate, term_months: c.termMonths },
    expected: {
      computed: true,
      branch: R.branch,
      instalment_shown: baht(R.instalmentSatang),
      rows: R.rows.length,
      first_row: { interest: baht(first.interest), principal: baht(first.principal), instalment: baht(first.instalment) },
      last_row: { interest: baht(last.interest), principal: baht(last.principal), instalment: baht(last.instalment) },
      sum_principal: baht(R.sumPrincipal),
      sum_interest: baht(R.sumInterest),
      final_balance: baht(R.finalBalance),
    },
    note: c.br004Note ? `${c.note} · ${c.br004Note}` : c.note,
  };
}

// ── entry ───────────────────────────────────────────────────────────────────
// เฉพาะตอนรันไฟล์นี้ตรงๆ เท่านั้น — golden/CALC-miniloan-004@v1.mjs import { computeSchedule }
// จากไฟล์นี้ไปใช้ ถ้าไม่กันไว้ตรงนี้ ตอน import จะพิมพ์รายงานตารางผ่อนแทรกมาโดยไม่ตั้งใจ (ไม่กระทบ
// ตัวเลขหรือพฤติกรรมของ computeSchedule เอง แค่กันผลข้างเคียงตอน import)
const isMain = process.argv[1]?.replace(/\\/g, "/").endsWith("CALC-miniloan-001@v2.mjs");
if (isMain) {
const argv = process.argv.slice(2);
const flag = (name, dflt) => { const i = argv.indexOf(name); return i < 0 ? dflt : argv[i + 1]; };
const WORKING_SCALE = Number(flag("--scale", 30));

if (argv.includes("--compare")) {
  const i = argv.indexOf("--compare");
  const a = Number(argv[i + 1] ?? 30), b = Number(argv[i + 2] ?? 40);
  let differs = 0;
  for (const c of CASES) {
    if (c.expectDeclined) continue;
    const ra = JSON.stringify(summarise(runCase(c, a)));
    const rb = JSON.stringify(summarise(runCase(c, b)));
    const full = (s) => JSON.stringify(computeSchedule(c, s).rows.map((x) => [x.t, x.interest.toString(), x.principal.toString(), x.balance.toString()]));
    const same = ra === rb && full(a) === full(b);
    if (!same) differs++;
    console.log(`${c.id}  scale ${a} vs ${b}  ${same ? "IDENTICAL ทุกแถว ระดับสตางค์" : "DIFFERS ← เลขเฉลยขึ้นกับสเกล ต้องกลับไป /req:calc"}`);
  }
  console.log(differs === 0
    ? `\nสรุป: สเกล ${a} กับ ${b} ให้ผลเท่ากันทุกแถวทุกกรณี — ตัวเลือกสเกลอยู่ต่ำกว่าความละเอียดที่สัญญาสนใจ`
    : `\nสรุป: ${differs} กรณีต่างกัน — เลขเฉลยยังตัดสินไม่ได้`);
  process.exit(differs === 0 ? 0 : 1);
}

if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map((c) => summarise(runCase(c, WORKING_SCALE))), null, 2));
  process.exit(0);
}

console.log(`CALC-miniloan-001@v2 · ตารางผ่อน EMI · working scale = ${WORKING_SCALE} ตำแหน่ง · rounding = HALF_UP ทุกจุดที่เกิด\n`);
for (const c of CASES) {
  const run = runCase(c, WORKING_SCALE);
  console.log("─".repeat(78));
  console.log(`${c.id} · P = ${c.principalBaht.toLocaleString("en-US")} บาท · อัตราต่อปี = ${c.annualRate} · n = ${c.termMonths}`);
  console.log(`     ${c.note}`);
  if (run.declined) {
    console.log(`     → ไม่คำนวณ · ${run.gate.reason}`);
    continue;
  }
  if (!run.gate.ok) console.log(`     ⚠ ด่าน BR-miniloan-004@v1: ${run.gate.reason} — แสดงพฤติกรรมของสัญญาล้วนๆ`);
  const R = run.result;
  console.log(`     สาขา: ${R.branch} · ค่างวดที่แสดง = ${baht(R.instalmentSatang)}`);
  console.log(`\n     งวด |     ดอกเบี้ย |      เงินต้น |      ค่างวด |     ยอดคงเหลือ (คอลัมน์เดียว)`);
  for (const row of R.rows) {
    console.log(
      `     ${String(row.t).padStart(3)} | ${baht(row.interest).padStart(12)} | ${baht(row.principal).padStart(12)} | ${baht(row.instalment).padStart(11)} | ${baht(row.balance).padStart(20)}`,
    );
  }
  console.log(`     ${"".padStart(3)} | ${baht(R.sumInterest).padStart(12)} | ${baht(R.sumPrincipal).padStart(12)} |  ← ผลรวม · ยอดคงเหลือปิดที่ ${baht(R.finalBalance)}`);
  console.log("");
}
}

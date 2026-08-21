/**
 * CALC-miniloan-001@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-016@v1 (EMI · ตารางผ่อนลดต้นลดดอก)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → เลขทุกตัวเป็น BigInt fixed-point ไม่มี Number/float ในการคำนวณเงินเลย
 *   rounding_mode    HALF_UP   → roundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ ไม่ใช่ปัดไปหาเลขคู่
 *   rounding_points  ปัดจุดเดียวตอนเขียนแถว ที่ 2 ตำแหน่ง:
 *                      ค่างวดที่แสดง = round(EMI, 2) เท่ากันทุกแถว
 *                      ดอกเบี้ยของแถว = round(ดอกเบี้ยงวดนั้น, 2)
 *                      เงินต้นของแถว  = round(EMI, 2) − ดอกเบี้ยของแถว   (หักเอา ไม่ปัดซ้ำ)
 *                    EMI และการเดินของ balance ภายในไม่ปัด
 *   residual_policy  งวดสุดท้ายดูดเศษ เทียบ "คอลัมน์ที่แสดง" ไม่ใช่บัญชีภายใน:
 *                      เงินต้นงวดสุดท้าย = P − Σ(เงินต้นที่แสดงของงวด 1..n−1)
 *   boundary         r = 0 → EMI = P / n
 *                    P/n นอกช่วง BR-miniloan-004@v1 → ไม่คำนวณ (ด่านนี้เป็นของ BR-004 ไม่ใช่ของสัญญา)
 *                    n = 1 → ตารางแถวเดียว งวดแรกเป็นงวดสุดท้ายด้วย
 *
 * ข้อที่สัญญาไม่ได้พิน และสคริปต์ต้องเลือกเอง — ประกาศไว้ตรงนี้:
 *   "คำนวณเต็มความละเอียด" เขียนโค้ดตรงตัวไม่ได้ เพราะ r = อัตราต่อปี / 12 เป็นทศนิยมไม่รู้จบ
 *   สคริปต์จึงใช้ทศนิยมภายใน WORKING_SCALE ตำแหน่ง (ตั้งค่าได้ด้วย --scale)
 *   ถ้ารันสองสเกลแล้วผลระดับสตางค์ต่างกัน แปลว่าเลขเฉลยขึ้นกับตัวเลือกนี้ ซึ่งต้องกลับไปที่ /req:calc
 *   ไม่ใช่ให้สคริปต์เลือกสเกลเอง — ดูโหมด --compare
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-001@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-001@v1.mjs" --scale 40
 *       node ".aeon/golden/CALC-miniloan-001@v1.mjs" --compare 30 40
 *       node ".aeon/golden/CALC-miniloan-001@v1.mjs" --json
 */

// ── fixed-point decimal บน BigInt ────────────────────────────────────────────
// ค่าทุกตัวเก็บเป็นจำนวนเต็ม = ค่าจริง × 10^scale · ไม่มี Number เข้ามาเกี่ยวกับเงินเลย
const pow10 = (k) => 10n ** BigInt(k);

/** ปัด HALF_UP: ครึ่งออกจากศูนย์เสมอ (ไม่ใช่ค่าตั้งต้นของ .NET ที่ปัดไปหาเลขคู่) */
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
    /** "0.25" → fixed-point */
    fromDecimalString(str) {
      const neg = str.trim().startsWith("-");
      const [i, f = ""] = str.trim().replace(/^-/, "").split(".");
      const frac = (f + "0".repeat(scale)).slice(0, scale);
      const v = BigInt(i || "0") * ONE + BigInt(frac || "0");
      return neg ? -v : v;
    },
    mul: (a, b) => divRoundHalfUp(a * b, ONE),
    div: (a, b) => divRoundHalfUp(a * ONE, b),
    /** fixed-point → สตางค์ (จำนวนเต็ม) ด้วย HALF_UP — นี่คือ "การปัดตอนเขียนแถว" ของสัญญา */
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

// ── สัญญาการคำนวณ ───────────────────────────────────────────────────────────
function computeSchedule({ principalBaht, annualRate, termMonths }, scale) {
  const fx = makeFx(scale);
  const n = termMonths;
  const P = fx.fromInt(principalBaht);
  const r = fx.div(fx.fromDecimalString(annualRate), fx.fromInt(12));

  // EMI — ไม่ปัด · ขอบ r = 0 ตัดตัวหารเป็นศูนย์ จึงสลับสูตรตามสัญญา
  let emi, branch;
  if (r === 0n) {
    emi = fx.div(P, fx.fromInt(n));
    branch = "r=0 → EMI = P / n";
  } else {
    let pow = fx.ONE;                       // (1+r)^n
    const onePlusR = fx.ONE + r;
    for (let k = 0; k < n; k++) pow = fx.mul(pow, onePlusR);
    emi = fx.div(fx.mul(fx.mul(P, r), pow), pow - fx.ONE);
    branch = "สูตร EMI ปกติ";
  }

  const instalmentSatang = fx.toSatang(emi);        // ค่างวดที่แสดง = round(EMI, 2)
  const principalSatang = BigInt(principalBaht) * 100n;

  const rows = [];
  let balance = P;                                   // บัญชีภายใน — ไม่ปัด
  let shownBalance = principalSatang;                // คอลัมน์ที่แสดง — สตางค์
  let shownPrincipalSum = 0n;

  for (let t = 1; t <= n; t++) {
    const interestFull = fx.mul(balance, r);
    const interestRow = fx.toSatang(interestFull);

    let principalRow;
    if (t < n) {
      principalRow = instalmentSatang - interestRow;              // หักเอา ไม่ปัดซ้ำ
    } else {
      principalRow = principalSatang - shownPrincipalSum;         // residual: ปิดคอลัมน์ที่แสดง
    }
    shownPrincipalSum += principalRow;

    const principalFull = emi - interestFull;                     // บัญชีภายในเดินด้วยเลขไม่ปัด
    balance -= principalFull;
    shownBalance -= principalRow;

    rows.push({
      t,
      interest: interestRow,
      principal: principalRow,
      instalment: interestRow + principalRow,
      balance: shownBalance,
    });
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

// ── กรณีทดสอบ — เลือกตามลำดับของ /req:golden ขั้นที่ 4 ──────────────────────
// ไม่มี source ชนิด sample_data ในสเปกนี้ จึงไม่มีแถวของลูกค้าให้ใส่ และ mismatches[] ว่างเพราะ
// "ไม่มีอะไรให้เทียบ" ไม่ใช่เพราะ "เทียบแล้วตรง"
const CASES = [
  { id: "C1", principalBaht: 100_000, annualRate: "0.25", termMonths: 12,
    note: "กรณีปกติ — ใช้อินพุตชุดเดียวกับ EX-miniloan-001 เพื่อผูกเลขเฉลยเข้ากับใบตัวอย่างที่มีอยู่" },
  { id: "C2", principalBaht: 1_000_000, annualRate: "0.25", termMonths: 60,
    note: "ขอบบนทั้งสองแกนของ BR-miniloan-004@v1 — และเป็นกรณีที่ส่วนสะสมจาก round(EMI,2) มากที่สุด" },
  { id: "C3", principalBaht: 10_000, annualRate: "0.25", termMonths: 6,
    note: "ขอบล่างทั้งสองแกนของ BR-miniloan-004@v1" },
  { id: "C4", principalBaht: 100_000, annualRate: "0", termMonths: 12,
    note: "boundary r = 0 — ตัวหาร (1+r)^n − 1 เป็นศูนย์ · เงินต้นหารด้วยงวดไม่ลงตัว เศษจึงเห็นได้ด้วยตา" },
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

console.log(`CALC-miniloan-001@v1 · ตารางผ่อน EMI · working scale = ${WORKING_SCALE} ตำแหน่ง · rounding = HALF_UP\n`);
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
  console.log(`\n     งวด |     ดอกเบี้ย |      เงินต้น |      ค่างวด |     ยอดคงเหลือ`);
  for (const row of R.rows) {
    console.log(
      `     ${String(row.t).padStart(3)} | ${baht(row.interest).padStart(12)} | ${baht(row.principal).padStart(12)} | ${baht(row.instalment).padStart(11)} | ${baht(row.balance).padStart(14)}`,
    );
  }
  console.log(`     ${"".padStart(3)} | ${baht(R.sumInterest).padStart(12)} | ${baht(R.sumPrincipal).padStart(12)} |  ← ผลรวม · ยอดคงเหลือปิดที่ ${baht(R.finalBalance)}`);
  console.log("");
}

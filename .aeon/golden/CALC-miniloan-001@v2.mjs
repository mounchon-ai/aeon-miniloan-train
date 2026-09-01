/**
 * CALC-miniloan-001@v2 — answer key generator
 * ระบุกฎ: BR-miniloan-016@v1 (EMI · ตารางผ่อนลดต้นลดดอก)
 * เวอร์ชันนี้เกิดจาก CHG-miniloan-005: คลี่ความขัดกับ BR-miniloan-035@v1 (ปัดทันทีทุกจุด)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → เลขทุกตัวเป็น BigInt fixed-point ไม่มี Number/float ในการคำนวณเงินเลย
 *   rounding_mode    HALF_UP   → roundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ ไม่ใช่ปัดไปหาเลขคู่
 *   rounding_points  ปัดทันทีทุกจุดที่เกิดขึ้น — ไม่มีบัญชีภายในที่ละเอียดกว่าคอลัมน์ที่แสดงอีกต่อไป
 *                    มีคอลัมน์เดียว (satang) ที่ทุกกฎอ่านค่าได้:
 *                      ค่างวดที่แสดง = round(EMI, 2) เท่ากันทุกแถว (คำนวณครั้งเดียว ใช้ซ้ำทุกแถว)
 *                      ดอกเบี้ยของแถว = round(ยอดคงเหลือก่อนแถวนี้ × r, 2)   ← ยอดคงเหลือที่ใช้คือค่าที่ปัดแล้วจากแถวก่อน
 *                      เงินต้นของแถว  = round(EMI, 2) − ดอกเบี้ยของแถว   (หักเอา ไม่ปัดซ้ำ)
 *                      ยอดคงเหลือหลังแถว = ยอดคงเหลือก่อนแถวนี้ − เงินต้นของแถว   (ปัดแล้ว ไม่มีคู่ขนาน)
 *                    ข้อยกเว้นเดียว: r เอง (อัตราดอกเบี้ยต่อเดือน) ไม่ใช่ค่าเงิน จึงไม่ปัด — คำนวณด้วยความละเอียด
 *                    WORKING_SCALE เหมือน v1 เพื่อหา (1+r)^n ให้แม่น ก่อนนำไปคูณกับเงินที่ปัดแล้วในแต่ละแถว
 *   residual_policy  งวดสุดท้ายดูดเศษ: เงินต้นงวดสุดท้าย = P − Σ(เงินต้นของงวด 1..n−1)
 *                    (ไม่ต้องระบุ "เทียบกับคอลัมน์ที่แสดง" อีกต่อไป เพราะมีคอลัมน์เดียวเท่านั้น)
 *   boundary         r = 0 → EMI = P / n (ดอกเบี้ยทุกงวดเป็น 0 เพราะไม่มีบัญชีภายในให้ดอกเบี้ยเกิดจากเศษ)
 *                    P/n นอกช่วง BR-miniloan-004@v1 → ไม่คำนวณ (ด่านนี้เป็นของ BR-004 ไม่ใช่ของสัญญา)
 *                    n = 1 → ตารางแถวเดียว งวดแรกเป็นงวดสุดท้ายด้วย
 *
 * สิ่งที่ต่างจาก v1 โดยตั้งใจ: ไม่มี "บัญชีภายในเต็มความละเอียด" ให้เดินคู่กับคอลัมน์ที่แสดงอีกต่อไป —
 * แถวที่ 1 ของทุกเคสจึงเหมือน v1 เป๊ะ (ยังไม่มีรอบปัดสะสมให้ต่าง) แต่แถวที่ 2 เป็นต้นไปอาจต่างจาก
 * เลขเฉลยเดิมของ v1 ระดับสตางค์ เพราะดอกเบี้ยของแต่ละแถวคำนวณจากยอดคงเหลือที่ "ปัดแล้ว" ของแถวก่อน
 * ไม่ใช่จากบัญชีภายในที่ไม่ปัด — นี่คือผลลัพธ์ที่ต้องการ ไม่ใช่ข้อผิดพลาด: ปิดช่องว่างที่ Q-miniloan-016 ชี้ไว้
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-001@v2.mjs"
 *       node ".aeon/golden/CALC-miniloan-001@v2.mjs" --scale 40
 *       node ".aeon/golden/CALC-miniloan-001@v2.mjs" --compare 30 40
 *       node ".aeon/golden/CALC-miniloan-001@v2.mjs" --json
 */

// ── fixed-point decimal บน BigInt ────────────────────────────────────────────
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
    /** สตางค์ (จำนวนเต็ม, ปัดแล้ว) → fixed-point — ใช้ตอน "อ่านยอดคงเหลือที่ปัดแล้ว" กลับมาคูณ r */
    fromSatang: (satang) => (BigInt(satang) * ONE) / 100n,
    mul: (a, b) => divRoundHalfUp(a * b, ONE),
    div: (a, b) => divRoundHalfUp(a * ONE, b),
    /** fixed-point → สตางค์ (จำนวนเต็ม) ด้วย HALF_UP — "ปัดทันที" ของสัญญา v2 */
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

// ── สัญญาการคำนวณ (v2 — ปัดทันทีทุกจุด คอลัมน์เดียว) ─────────────────────────
function computeSchedule({ principalBaht, annualRate, termMonths }, scale) {
  const fx = makeFx(scale);
  const n = termMonths;
  const P = fx.fromInt(principalBaht);
  const r = fx.div(fx.fromDecimalString(annualRate), fx.fromInt(12));

  // EMI — ปัดทันที (BR-035: "EMI ปัดก่อน") · ใช้ค่านี้ค่าเดียวทุกแถวยกเว้นแถวสุดท้ายที่ดูดเศษ
  let emiRaw, branch;
  if (r === 0n) {
    emiRaw = fx.div(P, fx.fromInt(n));
    branch = "r=0 → EMI = P / n";
  } else {
    let pow = fx.ONE;                       // (1+r)^n — r เองไม่ใช่ค่าเงิน ไม่ปัด
    const onePlusR = fx.ONE + r;
    for (let k = 0; k < n; k++) pow = fx.mul(pow, onePlusR);
    emiRaw = fx.div(fx.mul(fx.mul(P, r), pow), pow - fx.ONE);
    branch = "สูตร EMI ปกติ";
  }
  const instalmentSatang = fx.toSatang(emiRaw);       // ค่างวดที่แสดง = round(EMI, 2)

  const principalSatang = BigInt(principalBaht) * 100n;

  const rows = [];
  let balanceSatang = principalSatang;    // คอลัมน์เดียว — ปัดแล้วเสมอ ไม่มีบัญชีคู่ขนาน
  let principalSum = 0n;

  for (let t = 1; t <= n; t++) {
    const balanceFx = fx.fromSatang(balanceSatang);        // อ่านยอดคงเหลือที่ปัดแล้วของแถวก่อน
    const interestSatang = fx.toSatang(fx.mul(balanceFx, r)); // ปัดทันที

    let principalRow;
    if (t < n) {
      principalRow = instalmentSatang - interestSatang;         // หักเอา ไม่ปัดซ้ำ
    } else {
      principalRow = principalSatang - principalSum;            // residual: ปิดคอลัมน์เดียวที่มี
    }
    principalSum += principalRow;
    balanceSatang -= principalRow;                               // เดินด้วยค่าที่ปัดแล้วเท่านั้น

    rows.push({
      t,
      interest: interestSatang,
      principal: principalRow,
      instalment: interestSatang + principalRow,
      balance: balanceSatang,
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
// ไม่มี source ชนิด sample_data ในสเปกนี้ จึงไม่มีแถวของลูกค้าให้ใส่ · เคสเดียวกับ GD-miniloan-001
// (v1) โดยตั้งใจ เพื่อให้เทียบก่อน-หลังได้ตรงเคสต่อเคส
const CASES = [
  { id: "C1", principalBaht: 100_000, annualRate: "0.25", termMonths: 12,
    note: "กรณีปกติ — ใช้อินพุตชุดเดียวกับ EX-miniloan-001 เพื่อผูกเลขเฉลยเข้ากับใบตัวอย่างที่มีอยู่ · เทียบตรงกับ C1 ของ GD-miniloan-001 (v1)" },
  { id: "C2", principalBaht: 1_000_000, annualRate: "0.25", termMonths: 60,
    note: "ขอบบนทั้งสองแกนของ BR-miniloan-004@v1 — เคสที่ v1 สะสมส่วนต่างมากสุด (~0.17 บาท) เทียบว่า v2 ยังสะสมอยู่ไหม" },
  { id: "C3", principalBaht: 10_000, annualRate: "0.25", termMonths: 6,
    note: "ขอบล่างทั้งสองแกนของ BR-miniloan-004@v1" },
  { id: "C4", principalBaht: 100_000, annualRate: "0", termMonths: 12,
    note: "boundary r = 0 — เคสที่ Q-miniloan-016 ชี้ให้เห็นส่วนต่างชัดที่สุดใน v1 (งวดที่ 6: แสดง 50,000.02 vs ภายใน 50,000.00) — v2 ไม่มีบัญชีภายในให้ต่างอีกแล้ว" },
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

if (argv.includes("--diff-v1")) {
  // เทียบกับตัวเลขที่ verified แล้วของ GD-miniloan-001 (v1) เพื่อยืนยันว่าต่างเฉพาะที่คาดไว้
  const V1 = {
    C1: { first: { interest: "2,083.33", principal: "7,421.09", instalment: "9,504.42" }, last: { interest: "193.97", principal: "9,310.46", instalment: "9,504.43" }, sum_principal: "100,000.00", sum_interest: "14,053.05" },
    C2: { first: { interest: "20,833.33", principal: "8,517.99", instalment: "29,351.32" }, last: { interest: "599.01", principal: "28,752.48", instalment: "29,351.49" }, sum_principal: "1,000,000.00", sum_interest: "761,079.37" },
    C3: { first: { interest: "208.33", principal: "1,581.95", instalment: "1,790.28" }, last: { interest: "36.54", principal: "1,753.76", instalment: "1,790.30" }, sum_principal: "10,000.00", sum_interest: "741.70" },
    C4: { first: { interest: "0.00", principal: "8,333.33", instalment: "8,333.33" }, last: { interest: "0.00", principal: "8,333.37", instalment: "8,333.37" }, sum_principal: "100,000.00", sum_interest: "0.00" },
    C5: { first: { interest: "2,083.33", principal: "100,000.00", instalment: "102,083.33" }, last: { interest: "2,083.33", principal: "100,000.00", instalment: "102,083.33" }, sum_principal: "100,000.00", sum_interest: "2,083.33" },
  };
  for (const c of CASES) {
    if (c.expectDeclined) continue;
    const s = summarise(runCase(c, WORKING_SCALE));
    const v1 = V1[c.id];
    const sameFirst = JSON.stringify(s.expected.first_row) === JSON.stringify(v1.first);
    const sameLast = JSON.stringify(s.expected.last_row) === JSON.stringify(v1.last);
    const sameSumP = s.expected.sum_principal === v1.sum_principal;
    const sameSumI = s.expected.sum_interest === v1.sum_interest;
    console.log(`${c.id}: first_row ${sameFirst ? "เหมือน v1" : "ต่างจาก v1"} · last_row ${sameLast ? "เหมือน v1" : "ต่างจาก v1"} · sum_principal ${sameSumP ? "เหมือน v1" : "ต่างจาก v1"} · sum_interest ${sameSumI ? "เหมือน v1" : "ต่างจาก v1"}`);
  }
  process.exit(0);
}

if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map((c) => summarise(runCase(c, WORKING_SCALE))), null, 2));
  process.exit(0);
}

console.log(`CALC-miniloan-001@v2 · ตารางผ่อน EMI · ปัดทันทีทุกจุด (ไม่มีบัญชีภายใน) · working scale = ${WORKING_SCALE} ตำแหน่ง · rounding = HALF_UP\n`);
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

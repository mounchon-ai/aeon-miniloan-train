/**
 * CALC-miniloan-003@v1 — answer key generator
 * ระบุกฎ: BR-miniloan-050@v2 (ค่าธรรมเนียมการโปะ 1% หักออกจากส่วนเกินก่อนนำไปตัดเงินต้น)
 *
 * ทำตามสัญญาการคำนวณทีละช่อง ห้ามแก้เอง:
 *   numeric_type     decimal   → BigInt satang (จำนวนเต็ม) ไม่มี Number/float แตะเงินเลย
 *   rounding_mode    HALF_UP   → divRoundHalfUp() ปัดครึ่งออกจากศูนย์เสมอ
 *   rounding_points  ปัดทันทีที่ค่าธรรมเนียมคำนวณเสร็จ (2 ตำแหน่ง) ก่อนนำไปลบออกจากส่วนเกิน
 *                    ผลต่าง (principal_cut) ไม่ปัดซ้ำ — ค่าธรรมเนียมการโปะ = round(ส่วนเกิน × 1%, 2)
 *                    เงินต้นที่ลดลง = ส่วนเกิน − ค่าธรรมเนียมที่ปัดแล้ว
 *   ลำดับที่ผูกพัน   ปัดค่าธรรมเนียม 1% ก่อน แล้วค่อยลบออกจากส่วนเกิน — "(× 99%)" ในถ้อยคำกฎเป็นคำอธิบาย
 *                    โดยประมาณเท่านั้น ไม่ใช่เส้นทางคำนวณคู่ขนาน (ยืนยันผ่าน SRC-019 [1])
 *   residual_policy  ไม่มี — หักครั้งเดียวก้อนเดียว ไม่มีงวดให้แบ่ง
 *   boundary         ส่วนเกิน = 0 → ค่าธรรมเนียม = 0, เงินต้นลด = 0 (ไม่ใช่เหตุการณ์โปะ)
 *                    principal_cut > เงินต้นคงเหลือ → ปฏิเสธทั้งรายการตาม BR-miniloan-052@v2
 *                    principal_cut = เงินต้นคงเหลือพอดี → อนุญาต ไม่ปฏิเสธ
 *
 * remaining_principal เป็น input ที่ใช้ "เทียบ boundary" เท่านั้น ไม่ใช่ตัวแปรในสูตรค่าธรรมเนียม/เงินต้นลด —
 * ค่าที่ EX-miniloan-111/112/152 ไม่ได้ระบุไว้ตรงๆ จึงสมมติเป็นเลขกลมที่มากพอไม่ให้ชนขอบ (ระบุไว้ใน note ของแต่ละกรณี)
 *
 * ไม่มี source ชนิด sample_data ในสเปกนี้ → ไม่มีแถวของลูกค้าให้ใส่ ทุกแถวมาจาก boundary_behavior ของสัญญา
 * และเคสธรรมดาที่จับคู่กับใบตัวอย่าง (EX) ที่มีอยู่แล้ว
 *
 * รัน:  node ".aeon/golden/CALC-miniloan-003@v1.mjs"
 *       node ".aeon/golden/CALC-miniloan-003@v1.mjs" --json
 */

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

// ── สัญญาการคำนวณ ───────────────────────────────────────────────────────────
// surplusSatang / remainingPrincipalSatang เป็น BigInt satang อยู่แล้ว (money(2) แปลงเป็น minor unit
// ตั้งแต่ต้นทาง ไม่มีการ parse จาก float ที่ไหนเลยในสคริปต์นี้)
function computeToppingFee({ surplusSatang, remainingPrincipalSatang }) {
  // ค่าธรรมเนียม 1% ของส่วนเกิน ปัดทันทีที่คำนวณเสร็จ — surplusSatang/100 ปัด HALF_UP คือ round(surplus×1%,2)
  const feeSatang = divRoundHalfUp(surplusSatang, 100n);
  const principalCutSatang = surplusSatang - feeSatang;

  // การอ่านที่ไม่ถูกต้อง (ปัดผลคูณ 99% โดยตรง = round(surplus × 0.99)) — คำนวณไว้เฉพาะเพื่อโชว์ส่วนต่างใน
  // กรณีที่แยกสองทางออกจากกัน ไม่ใช่คำตอบของสัญญานี้ ห้ามใช้เป็นค่าที่ระบบคืน
  const rejectedAltCutSatang = divRoundHalfUp(surplusSatang * 99n, 100n);

  const accepted = principalCutSatang <= remainingPrincipalSatang;

  return {
    feeSatang,
    principalCutSatang,
    rejectedAltCutSatang,
    accepted,
    reason: accepted
      ? null
      : `principal_cut (${baht(principalCutSatang)}) > remaining_principal (${baht(remainingPrincipalSatang)}) → ปฏิเสธการบันทึกทั้งรายการตาม BR-miniloan-052@v2`,
  };
}

// ── กรณีทดสอบ — เลือกตามลำดับของ /req:golden ขั้นที่ 4 ──────────────────────
// ไม่มี source ชนิด sample_data ในสเปกนี้ จึงไม่มีแถวของลูกค้าให้ใส่
const CASES = [
  { id: "C1", surplusSatang: 2_000_000n, remainingPrincipalSatang: 10_000_000n,
    note: "ปกติ — เดียวกับ EX-miniloan-111 (ส่วนเกิน 20,000.00 บาท) · เงินต้นคงเหลือ 100,000.00 บาท เป็นค่าที่สมมติไว้ให้มากพอ ใบตัวอย่างไม่ได้ระบุตัวเลขนี้" },
  { id: "C2", surplusSatang: 1n, remainingPrincipalSatang: 10_000_000n,
    note: "boundary ส่วนเกินน้อยที่สุดที่ระบบรับ (0.01 บาท) — เดียวกับ EX-miniloan-112 · ค่าธรรมเนียม 1% ของ 0.01 = 0.0001 ปัดลงเป็น 0.00" },
  { id: "C3", surplusSatang: 505_051n, remainingPrincipalSatang: 500_000n,
    note: "boundary principal_cut เท่ากับเงินต้นคงเหลือพอดี — เดียวกับ EX-miniloan-148 (ส่วนเกิน 5,050.51 · เงินต้นคงเหลือ 5,000.00) → ยังรับได้" },
  { id: "C4", surplusSatang: 505_052n, remainingPrincipalSatang: 500_000n,
    note: "boundary principal_cut มากกว่าเงินต้นคงเหลือหนึ่งสตางค์ — เดียวกับ EX-miniloan-149 (ส่วนเกิน 5,050.52 · เงินต้นคงเหลือ 5,000.00) → ปฏิเสธตาม BR-miniloan-052@v2" },
  { id: "C5", surplusSatang: 150n, remainingPrincipalSatang: 10_000_000n,
    note: "boundary ลำดับการปัด — เดียวกับ EX-miniloan-152 (ส่วนเกิน 1.50 บาท) ค่าธรรมเนียมดิบ 0.0150 ตกกึ่งสตางค์พอดี: ปัดค่าธรรมเนียมก่อน (สัญญานี้) ให้ 1.48 · ถ้าปัดผลคูณ 99% โดยตรง (การอ่านที่ไม่ถูกต้อง) จะได้ 1.49 แทน" },
  { id: "C6", surplusSatang: 0n, remainingPrincipalSatang: 10_000_000n,
    note: "boundary ส่วนเกิน = 0 — ชำระพอดีตามยอดงวด ไม่มีการโปะ สูตรรองรับเองโดยไม่ต้องกันพิเศษ ไม่ถือเป็นเหตุการณ์โปะ" },
];

function summarise(c) {
  const R = computeToppingFee(c);
  const row = {
    id: c.id,
    input: { surplus_amount: baht(c.surplusSatang), remaining_principal: baht(c.remainingPrincipalSatang) },
    expected: {
      fee: baht(R.feeSatang),
      principal_cut: baht(R.principalCutSatang),
      accepted: R.accepted,
      reject_reason: R.reason,
    },
    note: c.note,
  };
  if (c.id === "C5") {
    row.expected.rejected_alt_reading_cut_if_rounded_99pct_directly = baht(R.rejectedAltCutSatang);
  }
  return row;
}

const argv = process.argv.slice(2);
if (argv.includes("--json")) {
  console.log(JSON.stringify(CASES.map(summarise), null, 2));
  process.exit(0);
}

console.log(`CALC-miniloan-003@v1 · ค่าธรรมเนียมการโปะ 1% หักออกจากส่วนเกินก่อนนำไปตัดเงินต้น · rounding = HALF_UP ทันทีที่ค่าธรรมเนียม\n`);
for (const c of CASES) {
  const R = computeToppingFee(c);
  console.log("─".repeat(78));
  console.log(`${c.id} · ส่วนเกิน ${baht(c.surplusSatang)} บาท · เงินต้นคงเหลือ ${baht(c.remainingPrincipalSatang)} บาท`);
  console.log(`     ${c.note}`);
  console.log(`     ค่าธรรมเนียมการโปะ 1% = ${baht(R.feeSatang)}`);
  console.log(`     เงินต้นที่ลดลง (principal_cut) = ${baht(R.principalCutSatang)}`);
  if (c.id === "C5") {
    console.log(`     [เทียบ] ถ้าปัดผลคูณ 99% โดยตรง (อ่านผิด) จะได้ = ${baht(R.rejectedAltCutSatang)}`);
  }
  console.log(`     ผล = ${R.accepted ? "รับชำระตามปกติ" : `ปฏิเสธ — ${R.reason}`}`);
  console.log("");
}

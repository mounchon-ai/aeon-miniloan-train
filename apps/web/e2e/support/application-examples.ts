/**
 * Reuses req's own Example Mapping rows (EX-miniloan-01x, `.aeon/req/requirements.json`) as UI test
 * data instead of inventing new numbers — those examples are already the vetted representatives of
 * each equivalence class (happy · boundary · exception) for BR-miniloan-001@v1, and each carries
 * `hasUi: true`, meaning req itself expects it provable on screen. requestedAmount/termMonths stay
 * fixed across all of them since none of these examples is testing BR-miniloan-004@v1 — only age,
 * income and employment vary, which is exactly what BR-miniloan-001@v1 checks.
 */
export interface ApplicationExample {
  /** The req example this case's numbers come from — kept so a failure traces back to its source. */
  exId: string;
  label: string;
  fields: {
    fullName: string;
    age: string;
    currentEmploymentMonths: string;
    monthlyIncome: string;
    existingMonthlyDebt: string;
    requestedAmount: string;
    requestedTermMonths: string;
  };
}

const BASE_LOAN = { existingMonthlyDebt: '0', requestedAmount: '100000', requestedTermMonths: '10' };

/** All three eligibility criteria pass — BR-miniloan-031@v2 auto-advances these to UnderReview. */
export const ELIGIBLE_EXAMPLES: readonly ApplicationExample[] = [
  {
    exId: 'EX-miniloan-015',
    label: 'ผ่านเกณฑ์ชัดเจน — อายุ 35 · รายได้ 30,000 · อายุงาน 24 เดือน',
    fields: { fullName: 'ตัวอย่าง EX-015', age: '35', currentEmploymentMonths: '24', monthlyIncome: '30000', ...BASE_LOAN },
  },
  {
    exId: 'EX-miniloan-016',
    label: 'ชนขอบล่างพอดีทั้งสามเกณฑ์ — อายุ 20 · รายได้ 15,000 · อายุงาน 4 เดือน',
    fields: { fullName: 'ตัวอย่าง EX-016', age: '20', currentEmploymentMonths: '4', monthlyIncome: '15000', ...BASE_LOAN },
  },
  {
    exId: 'EX-miniloan-018a',
    label: 'ขอบบนอายุพอดี 60 ปี — ยังผ่าน',
    fields: { fullName: 'ตัวอย่าง EX-018a', age: '60', currentEmploymentMonths: '24', monthlyIncome: '30000', ...BASE_LOAN },
  },
];

/**
 * Below the lower bound / above the upper bound of at least one criterion. EX-miniloan-017's own
 * wording says "ระบบปฏิเสธ" (the system refuses), but the design layer built since then (BR-miniloan-
 * 031@v2, SCN-miniloan-010) is more specific: a failed criterion lands Band C with reasons, and stays
 * on the officer's desk rather than auto-Rejecting. The assertion below checks that finer behaviour,
 * not EX-017's older, looser wording.
 */
export const INELIGIBLE_EXAMPLES: readonly ApplicationExample[] = [
  {
    exId: 'EX-miniloan-017',
    label: 'ต่ำกว่าขอบล่างทั้งสามเกณฑ์ — อายุ 19 · รายได้ 14,999 · อายุงาน 3 เดือน',
    fields: { fullName: 'ตัวอย่าง EX-017', age: '19', currentEmploymentMonths: '3', monthlyIncome: '14999', ...BASE_LOAN },
  },
  {
    exId: 'EX-miniloan-018b',
    label: 'เกินขอบบนอายุไปหนึ่งปี — อายุ 61',
    fields: { fullName: 'ตัวอย่าง EX-018b', age: '61', currentEmploymentMonths: '24', monthlyIncome: '30000', ...BASE_LOAN },
  },
];

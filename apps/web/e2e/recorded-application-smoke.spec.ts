import { expect, test, type Page } from '@playwright/test';

import { ELIGIBLE_EXAMPLES, INELIGIBLE_EXAMPLES, type ApplicationExample } from './support/application-examples';

async function fillAndSubmit(page: Page, fields: ApplicationExample['fields']): Promise<void> {
  await page.goto('http://localhost:3000/applications/new');

  await page.getByTestId('ui-miniloan-001-ent-001-full-name').fill(fields.fullName);
  await page.getByTestId('ui-miniloan-001-ent-002-age').fill(fields.age);
  await page.getByTestId('ui-miniloan-001-ent-002-current-employment-months').fill(fields.currentEmploymentMonths);
  await page.getByTestId('ui-miniloan-001-ent-002-monthly-income').fill(fields.monthlyIncome);
  await page.getByTestId('ui-miniloan-001-ent-002-existing-monthly-debt').fill(fields.existingMonthlyDebt);
  await page.getByTestId('ui-miniloan-001-ent-002-requested-amount').fill(fields.requestedAmount);
  await page.getByTestId('ui-miniloan-001-ent-002-requested-term-months').fill(fields.requestedTermMonths);

  await page.getByTestId('ui-miniloan-001-save-draft').click();
  await expect(page.locator('.application-form__message')).toHaveText('บันทึกร่างเรียบร้อย');

  // GAP-miniloan-025: submit stays disabled until a draft id exists — save-draft above is what
  // unlocks it, not a client validation this page runs on its own.
  await page.getByTestId('ui-miniloan-001-submit').click();
  await expect(page).toHaveURL(/\/applications\/[0-9a-f-]{36}$/);
}

/**
 * Cleaned up from a manual `npx playwright codegen` recording against the real /applications/new
 * screen (2026-09-09) — kept as its own case, exactly as typed, alongside the data-driven cases below
 * pulled from req's Example Mapping rows.
 */
test('บันทึก และยื่นใบสมัคร (จากการ record ด้วยมือ)', async ({ page }) => {
  await fillAndSubmit(page, {
    fullName: 'กกกก',
    age: '25',
    currentEmploymentMonths: '21',
    monthlyIncome: '50000',
    existingMonthlyDebt: '10000',
    requestedAmount: '100000',
    requestedTermMonths: '10',
  });

  // DTI ≈ 42% with these figures — Band A (BR-miniloan-006@v1), same shape as SCN-miniloan-004.
  await expect(page.getByTestId('ui-miniloan-003-ent-002-status')).toHaveText(/Submitted|UnderReview/);
});

test.describe('BR-miniloan-001@v1 — เกณฑ์คุณสมบัติ (data-driven จาก req Example Mapping)', () => {
  for (const example of ELIGIBLE_EXAMPLES) {
    test(`${example.exId}: ${example.label} → ผ่านเกณฑ์ ไป UnderReview อัตโนมัติ`, async ({ page }) => {
      await fillAndSubmit(page, example.fields);
      await expect(page.getByTestId('ui-miniloan-003-ent-002-status')).toHaveText(/Submitted|UnderReview/);
    });
  }

  for (const example of INELIGIBLE_EXAMPLES) {
    test(`${example.exId}: ${example.label} → ตกเกณฑ์ อย่างน้อยหนึ่งข้อ`, async ({ page }) => {
      await fillAndSubmit(page, example.fields);
      // SCN-miniloan-010: a failed criterion lands Band C with reasons and stays on the officer's
      // desk — it must NOT auto-advance to UnderReview the way a passing Band A/B application does.
      await expect(page.getByTestId('ui-miniloan-003-ent-002-status')).not.toHaveText('UnderReview');
    });
  }
});

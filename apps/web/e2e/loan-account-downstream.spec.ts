import { expect, test } from '@playwright/test';

import { createDisbursedAccount } from './support/api';
import { loginAs, ROLE } from './support/roles';

/**
 * Screens downstream of disbursement — reached here by carrying an application to Disbursed via
 * the API (support/api.ts) rather than by re-driving the approve/disburse UI already covered in
 * loan-officer-review.spec.ts. Each test below is itself pure UI: it asserts what the screen shows
 * and does after a real click, nothing more.
 */
test.describe('ดูตารางผ่อนและบันทึกการชำระ', () => {
  test('SCN-miniloan-042: ผู้สมัครดูตารางผ่อนชำระของบัญชีตนเอง', async ({ page, request }) => {
    const { accountNumber } = await createDisbursedAccount(request);

    await loginAs(page, ROLE.APPLICANT, `/loan-accounts/${accountNumber}/schedule`);

    const rows = page.getByTestId('ui-miniloan-004-ent-008-installment-number');
    await expect(rows).toHaveCount(12); // BAND_A_APPLICANT.requestedTermMonths
    await expect(rows.first()).toHaveText('1');
  });

  test('SCN-miniloan-044: เรียกตารางผ่อนของบัญชีคนอื่นถูกปฏิเสธ', async ({ page, request }) => {
    const { accountNumber } = await createDisbursedAccount(request);

    // ROLE-002 is neither this account's applicant (ROLE-001) nor Operations (ROLE-004) —
    // BR-miniloan-033@v1's scope has no entry for it at all.
    await loginAs(page, ROLE.LOAN_OFFICER, `/loan-accounts/${accountNumber}/schedule`);

    await expect(page.locator('.repayment-schedule__unauthorized, .repayment-schedule__error')).toBeVisible();
    await expect(page.getByTestId('ui-miniloan-004-ent-008-installment-number')).toHaveCount(0);
  });

  test('SCN-miniloan-045: บันทึกการชำระตรงยอดงวดแรกพอดี ปิดงวดนั้น', async ({ page, request }) => {
    const { accountNumber } = await createDisbursedAccount(request);

    await loginAs(page, ROLE.OPERATIONS, `/loan-accounts/${accountNumber}`);

    const firstDueRow = page.locator('.account-detail__table tbody tr').first();
    const emiAmount = await firstDueRow.getByTestId('ui-miniloan-012-ent-008-emi-amount').innerText();
    const outstandingBefore = await page
      .getByTestId('ui-miniloan-012-ent-006-outstanding-principal')
      .innerText();

    await page.getByTestId('ui-miniloan-012-ent-009-amount').fill(emiAmount);
    await page.getByTestId('ui-miniloan-012-record-payment').click();

    // Not asserting on `.payment-form__outcome` here: recordPayment()'s success handler emits
    // `recorded`, whose parent handler (account-detail.component.ts's `load()`) sets `loading` to
    // true in the same tick, which unmounts app-record-payment (the @else-if branch it lives in)
    // before that message ever paints. The persisted, actually-observable effects below are what a
    // user sees once the reload completes.
    await expect(firstDueRow.getByTestId('ui-miniloan-012-ent-008-status')).not.toHaveText(/ค้าง/);
    await expect(page.getByTestId('ui-miniloan-012-ent-006-outstanding-principal')).not.toHaveText(
      outstandingBefore,
    );
  });

  test('SCN-miniloan-047: ชำระน้อยกว่ายอดงวด ระบบปฏิเสธและไม่ปิดงวด', async ({ page, request }) => {
    const { accountNumber } = await createDisbursedAccount(request);

    await loginAs(page, ROLE.OPERATIONS, `/loan-accounts/${accountNumber}`);

    const firstDueRow = page.locator('.account-detail__table tbody tr').first();
    const emiAmount = await firstDueRow.getByTestId('ui-miniloan-012-ent-008-emi-amount').innerText();
    const shortAmount = (Number(emiAmount.replace(/,/g, '')) - 1).toString();

    await page.getByTestId('ui-miniloan-012-ent-009-amount').fill(shortAmount);
    await page.getByTestId('ui-miniloan-012-record-payment').click();

    await expect(page.locator('.payment-form__error')).toBeVisible();
    await expect(firstDueRow.getByTestId('ui-miniloan-012-ent-008-status')).toHaveText(/ค้าง/);
  });
});

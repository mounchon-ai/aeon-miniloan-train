import { createAssignedApplication } from './support/api';
import { expect, test } from './support/screenshot';
import { loginAs, ROLE } from './support/roles';

/**
 * UI-miniloan-007 (พิจารณาใบสมัคร), reached directly at /review/:id rather than by clicking through
 * /review's own list — the queue link itself is exercised by the assign step this suite's setup
 * bypasses (see support/api.ts's header comment on GAP-miniloan-017/018). Approve, reject and
 * disburse are real, working screens; only the supervisor's hand-off to get here is broken today.
 */
test.describe('พิจารณาอนุมัติ/ปฏิเสธ/เบิกจ่าย (Loan Officer)', () => {
  test('SCN-miniloan-016: อนุมัติใบสมัครที่ถูกมอบหมายให้ตนเอง', async ({ page, request }) => {
    const applicationId = await createAssignedApplication(request);

    await loginAs(page, ROLE.LOAN_OFFICER, `/review/${applicationId}`);

    await expect(page.getByTestId('ui-miniloan-007-ent-003-band')).toHaveText('A');
    await page.getByTestId('ui-miniloan-007-approve').click();

    await expect(page.locator('.application-review__outcome')).toBeVisible();
    await expect(page.locator('.application-review__status')).toContainText('Approved');
  });

  test('SCN-miniloan-021: ปฏิเสธใบสมัครพร้อมระบุเหตุผล', async ({ page, request }) => {
    const applicationId = await createAssignedApplication(request);

    await loginAs(page, ROLE.LOAN_OFFICER, `/review/${applicationId}`);

    await page.getByTestId('ui-miniloan-007-ent-002-rejection-reason').fill('รายได้ไม่สม่ำเสมอ');
    await page.getByTestId('ui-miniloan-007-reject').click();

    await expect(page.locator('.application-review__outcome')).toBeVisible();
    await expect(page.locator('.application-review__status')).toContainText('Rejected');
  });

  test('SCN-miniloan-023: ปฏิเสธโดยไม่ระบุเหตุผลถูกปฏิเสธ ใบสมัครไม่เปลี่ยนสถานะ', async ({
    page,
    request,
  }) => {
    const applicationId = await createAssignedApplication(request);

    await loginAs(page, ROLE.LOAN_OFFICER, `/review/${applicationId}`);

    await page.getByTestId('ui-miniloan-007-reject').click();

    await expect(page.locator('.application-review__command-error')).toBeVisible();
    await expect(page.locator('.application-review__status')).toContainText('UnderReview');
  });

  test('SCN-miniloan-033: เบิกจ่ายใบสมัครที่อนุมัติแล้ว สร้างบัญชีสินเชื่อ', async ({ page, request }) => {
    const applicationId = await createAssignedApplication(request);

    await loginAs(page, ROLE.LOAN_OFFICER, `/review/${applicationId}`);
    await page.getByTestId('ui-miniloan-007-approve').click();
    await expect(page.locator('.application-review__status')).toContainText('Approved');

    await page.getByTestId('ui-miniloan-007-disburse').click();

    await expect(page.locator('.application-review__outcome')).toBeVisible();
    await expect(page.locator('.application-review__status')).toContainText('Disbursed');
  });
});

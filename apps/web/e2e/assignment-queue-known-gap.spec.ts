import { expect, test } from '@playwright/test';

import { createSubmittedApplication, uniqueApplicant } from './support/api';
import { loginAs, ROLE } from './support/roles';

/**
 * UI-miniloan-010's assign-officer and cancel-application buttons are DECLARED but cannot succeed
 * today, on purpose and on record: unassigned-queue.component.ts's own class doc, and GAP-miniloan-017
 * / GAP-miniloan-018 (open questions to design as of this writing) explain why — no field on this
 * screen captures a Loan Officer id, no endpoint returns a roster to choose from, and no field
 * captures a cancellation reason either. FE-miniloan-024 built it to send empty values and let the
 * API's own refusal prove BR-miniloan-032@v1 / BR-miniloan-047@v1 are enforced there rather than only
 * hidden behind a control.
 *
 * This spec is a CHARACTERIZATION test of that known, accepted-for-now state, not a happy-path test.
 * If it starts failing, that almost certainly means GAP-miniloan-017/018 were answered and this file
 * (and support/api.ts's assignToLoanOfficer bypass) need updating to match the real picker/roster.
 */
test.describe('มอบหมาย/ยกเลิกจากคิวมอบหมาย — ช่องว่างที่ design ยังไม่ตอบ', () => {
  test('SCN-miniloan-014-related: assign-officer จากคิวส่งค่าว่างเสมอ ถูก API ปฏิเสธ', async ({
    page,
    request,
  }) => {
    const fields = uniqueApplicant('GAP-017 assign');
    await createSubmittedApplication(request, fields);

    await loginAs(page, ROLE.SUPERVISOR, '/assignment-queue');

    const row = page
      .locator('.unassigned-queue__row')
      .filter({ has: page.getByText(fields.fullName) });
    await row.getByTestId('ui-miniloan-010-assign-officer').click();

    await expect(page.locator('.unassigned-queue__command-error')).toBeVisible();
    // The row is still here — a refused command must not have quietly assigned it anyway.
    await expect(row).toBeVisible();
  });

  test('GAP-miniloan-018: cancel-application จากคิวส่งเหตุผลว่างเสมอ ถูก API ปฏิเสธ', async ({
    page,
    request,
  }) => {
    const fields = uniqueApplicant('GAP-018 cancel');
    await createSubmittedApplication(request, fields);

    await loginAs(page, ROLE.SUPERVISOR, '/assignment-queue');

    const row = page
      .locator('.unassigned-queue__row')
      .filter({ has: page.getByText(fields.fullName) });
    await row.getByTestId('ui-miniloan-010-cancel-application').click();

    await expect(page.locator('.unassigned-queue__command-error')).toBeVisible();
    await expect(row).toBeVisible();
  });
});

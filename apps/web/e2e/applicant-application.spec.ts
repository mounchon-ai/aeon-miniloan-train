import { expect, test } from '@playwright/test';

import { BAND_A_APPLICANT } from './support/api';
import { loginAs, ROLE } from './support/roles';

/**
 * UI-miniloan-001 (ยื่นใบสมัครใหม่), driven end to end through the real screen and the real API —
 * no API-level setup here, unlike the specs downstream of assignment. Field values render this
 * component's own contract: application-form.component.ts sends whatever is typed with no client
 * validation (BR-miniloan-027@v1 — the API is the only judge), so every assertion here reads the
 * API's own message/status back off the page rather than a client-side rule.
 */
test.describe('ยื่นใบสมัครสินเชื่อ (Applicant)', () => {
  test('SCN-miniloan-001: บันทึกร่างด้วยข้อมูลบางส่วน ไม่ตรวจกฎธุรกิจ', async ({ page }) => {
    await loginAs(page, ROLE.APPLICANT, '/applications/new');

    await page.getByTestId('ui-miniloan-001-ent-001-full-name').fill('ผู้สมัครกรอกร่าง');
    await page.getByTestId('ui-miniloan-001-ent-002-age').fill('30');
    // Everything else left empty on purpose — BR-miniloan-008@v1: a draft saves as typed.

    await page.getByTestId('ui-miniloan-001-save-draft').click();

    await expect(page.locator('.application-form__message')).toHaveText('บันทึกร่างเรียบร้อย');
    // AC-miniloan-031: submit only unlocks once a draft id exists.
    await expect(page.getByTestId('ui-miniloan-001-submit')).toBeEnabled();
  });

  test('SCN-miniloan-004: กรอกครบทุกช่องแล้วยื่น เปลี่ยนสถานะและล็อกฟอร์ม', async ({ page }) => {
    await loginAs(page, ROLE.APPLICANT, '/applications/new');

    await page.getByTestId('ui-miniloan-001-ent-001-full-name').fill(BAND_A_APPLICANT.fullName);
    await page.getByTestId('ui-miniloan-001-ent-002-age').fill(String(BAND_A_APPLICANT.age));
    await page
      .getByTestId('ui-miniloan-001-ent-002-current-employment-months')
      .fill(String(BAND_A_APPLICANT.currentEmploymentMonths));
    await page
      .getByTestId('ui-miniloan-001-ent-002-monthly-income')
      .fill(String(BAND_A_APPLICANT.monthlyIncome));
    await page
      .getByTestId('ui-miniloan-001-ent-002-existing-monthly-debt')
      .fill(String(BAND_A_APPLICANT.existingMonthlyDebt));
    await page
      .getByTestId('ui-miniloan-001-ent-002-requested-amount')
      .fill(String(BAND_A_APPLICANT.requestedAmount));
    await page
      .getByTestId('ui-miniloan-001-ent-002-requested-term-months')
      .fill(String(BAND_A_APPLICANT.requestedTermMonths));

    await page.getByTestId('ui-miniloan-001-save-draft').click();
    await expect(page.locator('.application-form__message')).toHaveText('บันทึกร่างเรียบร้อย');

    await page.getByTestId('ui-miniloan-001-submit').click();

    // onSubmit() navigates to /applications/:id on success (application-form.component.ts).
    await expect(page).toHaveURL(/\/applications\/[0-9a-f-]{36}$/);
    // BR-miniloan-031@v2: Band A/B moves itself straight to UnderReview, with no button pressed.
    await expect(page.getByTestId('ui-miniloan-003-ent-002-status')).toHaveText(/Submitted|UnderReview/);
  });

  test('SCN-miniloan-006: ยื่นโดยกรอกไม่ครบ ถูกปฏิเสธและยังเป็น Draft', async ({ page }) => {
    await loginAs(page, ROLE.APPLICANT, '/applications/new');

    // Only a name — every other required field is left blank.
    await page.getByTestId('ui-miniloan-001-ent-001-full-name').fill('ผู้สมัครกรอกไม่ครบ');
    await page.getByTestId('ui-miniloan-001-save-draft').click();
    await expect(page.getByTestId('ui-miniloan-001-submit')).toBeEnabled();

    await page.getByTestId('ui-miniloan-001-submit').click();

    // AC-miniloan-030: the API names every missing field at once, and status stays Draft.
    await expect(page.locator('.application-form__field-errors li').first()).toBeVisible();
    await expect(page.getByTestId('ui-miniloan-001-submit')).toBeEnabled();
    await expect(page).toHaveURL(/\/applications\/new$/);
  });
});

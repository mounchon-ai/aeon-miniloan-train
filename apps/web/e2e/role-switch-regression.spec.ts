import { createSubmittedApplication, uniqueApplicant } from './support/api';
import { expect, test } from './support/screenshot';
import { loginAs, ROLE } from './support/roles';

/**
 * Regression for the bug fixed in commit 6666794 ("a role switch left the previous role's rows on
 * screen"): nav.component.ts's onRoleChange() now does a full page reload on a real role change so
 * every feature component re-fetches with the new token, instead of the old role's data just sitting
 * there. This drives the shell's actual role-switcher <select> (unlike every other spec's loginAs,
 * which seeds sessionStorage directly) because that reload path is exactly what is under test.
 *
 * What this spec deliberately does NOT assert: which screen a switch should land on. That is
 * GAP-miniloan-026, still open as of this writing — nobody has declared a role-switch destination,
 * so the URL stays wherever it was (here, /applications) and this only checks that the PREVIOUS
 * role's data is gone, not that /applications is the right place to be afterwards.
 */
test('role switch clears the previous role’s rows instead of leaving them on screen', async ({
  page,
  request,
}) => {
  const fields = uniqueApplicant('role-switch regression');
  await createSubmittedApplication(request, fields);

  await loginAs(page, ROLE.APPLICANT, '/applications');
  // The applicant's list can carry rows from other test runs too — this only needs at least one row
  // present before the switch, to be sure the "gone after switching" assertion below is meaningful.
  await expect(page.getByTestId('ui-miniloan-002-ent-002-status').first()).toBeVisible();

  await page.locator('.app-nav__role-switcher select').selectOption(ROLE.LOAN_OFFICER);

  // ROLE-002 has no scope over /applications' applicant-owned rows (BR-miniloan-033@v1) — whatever
  // the API answers, it must not be the applicant's own rows from before the switch. The old bug was
  // that these stayed on screen untouched; the fix is a reload that re-fetches with the new token.
  await expect(page.getByTestId('ui-miniloan-002-ent-002-status')).toHaveCount(0);
});

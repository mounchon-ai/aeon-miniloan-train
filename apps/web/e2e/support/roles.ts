import type { Page } from '@playwright/test';

/**
 * Mirrors apps/web/src/app/core/services/current-role.service.ts DEMO_ROLES. This is a mock/token
 * stand-in for auth (CLAUDE.md), not a real login — one fixed token per role.
 */
export const ROLE = {
  APPLICANT: 'ROLE-001',
  LOAN_OFFICER: 'ROLE-002',
  SUPERVISOR: 'ROLE-003',
  OPERATIONS: 'ROLE-004',
  ADJUSTMENT_APPROVER: 'ROLE-005',
} as const;

export type RoleId = (typeof ROLE)[keyof typeof ROLE];

/** current-role.service.ts DEMO_ROLES — role id to its fixed mock bearer token. */
export const TOKEN: Record<RoleId, string> = {
  [ROLE.APPLICANT]: 'mock-role-001',
  [ROLE.LOAN_OFFICER]: 'mock-role-002',
  [ROLE.SUPERVISOR]: 'mock-role-003',
  [ROLE.OPERATIONS]: 'mock-role-004',
  [ROLE.ADJUSTMENT_APPROVER]: 'mock-role-005',
};

const STORAGE_KEY = 'miniloan.demo-role';

/**
 * Seeds the demo role directly into sessionStorage and opens `path` as that role — CurrentRoleService
 * reads this key once, at construction, which a fresh navigation triggers. This skips the shell's
 * role-switcher <select> for tests that don't care about the switch itself; the regression spec for
 * the switch drives the real <select> instead so it exercises the actual reload path
 * (nav.component.ts onRoleChange).
 */
export async function loginAs(page: Page, roleId: RoleId, path: string): Promise<void> {
  await page.goto('/'); // land on the app's own origin before touching its sessionStorage
  await page.evaluate(
    ({ key, id }) => sessionStorage.setItem(key, id),
    { key: STORAGE_KEY, id: roleId },
  );
  await page.goto(path);
}

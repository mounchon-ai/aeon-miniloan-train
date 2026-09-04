import { Injectable, signal } from '@angular/core';

/**
 * Demo role registry. Real auth is out of scope this round (CLAUDE.md,
 * BR-miniloan-030@v1) and no login screen exists in the sitemap, so each
 * role carries a fixed mock token instead of one issued by a login flow.
 * A person picks their role from the shell's role switcher.
 */
export interface DemoRole {
  id: string;
  label: string;
  mockToken: string;
}

export const DEMO_ROLES: readonly DemoRole[] = [
  { id: 'ROLE-001', label: 'ผู้สมัคร', mockToken: 'mock-role-001' },
  { id: 'ROLE-002', label: 'เจ้าหน้าที่สินเชื่อ', mockToken: 'mock-role-002' },
  { id: 'ROLE-003', label: 'หัวหน้าเจ้าหน้าที่สินเชื่อ', mockToken: 'mock-role-003' },
  { id: 'ROLE-004', label: 'ฝ่ายปฏิบัติการ', mockToken: 'mock-role-004' },
  { id: 'ROLE-005', label: 'ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้ว', mockToken: 'mock-role-005' },
];

@Injectable({ providedIn: 'root' })
export class CurrentRoleService {
  private readonly current = signal<DemoRole>(DEMO_ROLES[0]);

  readonly role = this.current.asReadonly();

  readonly roles: readonly DemoRole[] = DEMO_ROLES;

  setRole(roleId: string): void {
    const next = DEMO_ROLES.find((role) => role.id === roleId);
    if (next) {
      this.current.set(next);
    }
  }
}

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

/**
 * Where the chosen role survives a reload. sessionStorage rather than
 * localStorage: this is a per-tab demo identity, not a saved preference, and
 * a second tab left on another role is easier to read than one that silently
 * followed the first.
 */
const STORAGE_KEY = 'miniloan.demo-role';

/**
 * Reads the role back, tolerating every way storage can refuse: a private
 * window, cleared site data, or a browser that throws on the accessor
 * itself. An unreadable store is not an error here — it means the first role
 * in the list, exactly as before anything was ever stored.
 */
function restoreRole(): DemoRole {
  try {
    const id = sessionStorage.getItem(STORAGE_KEY);
    return DEMO_ROLES.find((role) => role.id === id) ?? DEMO_ROLES[0];
  } catch {
    return DEMO_ROLES[0];
  }
}

@Injectable({ providedIn: 'root' })
export class CurrentRoleService {
  private readonly current = signal<DemoRole>(restoreRole());

  readonly role = this.current.asReadonly();

  readonly roles: readonly DemoRole[] = DEMO_ROLES;

  /**
   * Returns whether the identity actually moved. The shell reloads the app on a real switch, and
   * a reload nobody asked for — an id naming no role, or the role already selected — would throw
   * away whatever is on screen for nothing.
   */
  setRole(roleId: string): boolean {
    const next = DEMO_ROLES.find((role) => role.id === roleId);
    if (!next || next.id === this.current().id) {
      return false;
    }
    this.current.set(next);
    try {
      sessionStorage.setItem(STORAGE_KEY, next.id);
    } catch {
      // A store that refuses to write costs the role its survival across a reload and nothing
      // else — the signal above is what every caller in this session reads.
    }
    return true;
  }
}

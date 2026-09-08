import { TestBed } from '@angular/core/testing';

import { CurrentRoleService, DEMO_ROLES } from './current-role.service';

/**
 * The demo role has to survive a reload, because the shell reloads the app on every switch
 * (nav.component.ts) — without this the reload would land back on ROLE-001 and the switch would
 * look like it did nothing. sessionStorage rather than localStorage: a per-tab demo identity.
 */
describe('CurrentRoleService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });

  afterEach(() => sessionStorage.clear());

  it('starts on the first role when nothing was ever stored', () => {
    expect(TestBed.inject(CurrentRoleService).role().id).toBe(DEMO_ROLES[0].id);
  });

  it('writes the chosen role so the next page load can read it back', () => {
    TestBed.inject(CurrentRoleService).setRole('ROLE-003');

    expect(sessionStorage.getItem('miniloan.demo-role')).toBe('ROLE-003');
  });

  it('restores the stored role on a fresh instance — this is what a reload does', () => {
    sessionStorage.setItem('miniloan.demo-role', 'ROLE-005');
    TestBed.resetTestingModule();

    expect(TestBed.inject(CurrentRoleService).role().id).toBe('ROLE-005');
  });

  it('falls back to the first role when the stored value names nobody', () => {
    sessionStorage.setItem('miniloan.demo-role', 'ROLE-999');
    TestBed.resetTestingModule();

    expect(TestBed.inject(CurrentRoleService).role().id).toBe(DEMO_ROLES[0].id);
  });

  it('ignores an unknown role id and keeps the one it has', () => {
    const service = TestBed.inject(CurrentRoleService);
    service.setRole('ROLE-002');
    service.setRole('not-a-role');

    expect(service.role().id).toBe('ROLE-002');
  });
});

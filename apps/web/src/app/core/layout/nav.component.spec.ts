import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { NavComponent } from './nav.component';
import { CurrentRoleService } from '../services/current-role.service';

describe('NavComponent', () => {
  beforeEach(async () => {
    // the role now survives a reload (CurrentRoleService), so one test must not seed the next
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [NavComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('shows only the nav entry scoped to the default role (ROLE-001)', () => {
    const fixture = TestBed.createComponent(NavComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.entries()).toHaveLength(1);
    expect(fixture.componentInstance.entries()[0].screen).toBe('UI-miniloan-002');
  });

  it('switches the visible entry when the role changes', () => {
    const fixture = TestBed.createComponent(NavComponent);
    const currentRole = TestBed.inject(CurrentRoleService);
    fixture.detectChanges();

    currentRole.setRole('ROLE-004');
    fixture.detectChanges();

    expect(fixture.componentInstance.entries()).toHaveLength(1);
    expect(fixture.componentInstance.entries()[0].screen).toBe('UI-miniloan-011');
  });

  /**
   * qa selects the menu by these strings, so the shape has to be mock's — nav-<ui-id lowercase>,
   * the same value navControlId mints — and it has to be in the DOM, not only on the component.
   */
  it('renders each nav link with the testid mock mints for its destination screen', () => {
    const fixture = TestBed.createComponent(NavComponent);
    const currentRole = TestBed.inject(CurrentRoleService);
    fixture.detectChanges();

    const testidOf = () =>
      (fixture.nativeElement as HTMLElement)
        .querySelector('.app-nav__links a')
        ?.getAttribute('data-testid');

    expect(testidOf()).toBe('nav-ui-miniloan-002');

    currentRole.setRole('ROLE-004');
    fixture.detectChanges();
    expect(testidOf()).toBe('nav-ui-miniloan-011');
  });

  it('shows no nav entry for ROLE-005 (no entry:true screen assigned to it yet)', () => {
    const fixture = TestBed.createComponent(NavComponent);
    const currentRole = TestBed.inject(CurrentRoleService);
    fixture.detectChanges();

    currentRole.setRole('ROLE-005');
    fixture.detectChanges();

    expect(fixture.componentInstance.entries()).toHaveLength(0);
  });
  /**
   * Switching role changes who is asking. Every feature component loads once in its constructor
   * and listens to no role signal, so a switch that only flipped the signal left the previous
   * role's rows on screen — a Loan Officer kept reading the applicant's own list while
   * GET /applications answers [] for that token. The shell reloads instead of re-fetching in
   * eight places, and the choice is in sessionStorage before the reload so it comes back.
   */
  it('reloads the app when the role is switched, so no screen keeps the previous role data', () => {
    const reload = vi.fn();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: DOCUMENT, useValue: { defaultView: { location: { reload } } } },
      ],
    });

    const nav = TestBed.runInInjectionContext(() => new NavComponent());
    nav.onRoleChange('ROLE-002');

    expect(TestBed.inject(CurrentRoleService).role().id).toBe('ROLE-002');
    expect(sessionStorage.getItem('miniloan.demo-role')).toBe('ROLE-002');
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it('does not reload when the switcher names a role that does not exist', () => {
    const reload = vi.fn();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: DOCUMENT, useValue: { defaultView: { location: { reload } } } },
      ],
    });

    const nav = TestBed.runInInjectionContext(() => new NavComponent());
    nav.onRoleChange('ROLE-999');

    expect(TestBed.inject(CurrentRoleService).role().id).toBe('ROLE-001');
    expect(reload).not.toHaveBeenCalled();
  });
  /**
   * The box has to name the role the menu and the token are already using. Binding [value] on the
   * select lost that once the role survived a reload, because @for had not created the options yet
   * when Angular wrote the value.
   */
  it('shows the restored role in the switcher, not the first option', () => {
    sessionStorage.setItem('miniloan.demo-role', 'ROLE-004');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [NavComponent],
      providers: [provideRouter([])],
    });

    const fixture = TestBed.createComponent(NavComponent);
    fixture.detectChanges();

    const select = (fixture.nativeElement as HTMLElement).querySelector('select');
    expect(select?.value).toBe('ROLE-004');
    expect(fixture.componentInstance.entries()[0].screen).toBe('UI-miniloan-011');
  });
});

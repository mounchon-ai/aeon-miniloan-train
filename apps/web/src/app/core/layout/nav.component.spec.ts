import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { NavComponent } from './nav.component';
import { CurrentRoleService } from '../services/current-role.service';

describe('NavComponent', () => {
  beforeEach(async () => {
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

  it('shows no nav entry for ROLE-005 (no entry:true screen assigned to it yet)', () => {
    const fixture = TestBed.createComponent(NavComponent);
    const currentRole = TestBed.inject(CurrentRoleService);
    fixture.detectChanges();

    currentRole.setRole('ROLE-005');
    fixture.detectChanges();

    expect(fixture.componentInstance.entries()).toHaveLength(0);
  });
});

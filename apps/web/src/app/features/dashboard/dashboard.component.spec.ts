import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DashboardComponent } from './dashboard.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { DashboardSummary } from '../../core/services/dashboard.service';

/**
 * UI-miniloan-009 (UC-miniloan-020 · AC-miniloan-110 · AC-miniloan-111 · AC-miniloan-112).
 *
 * <p><b>What only a web test can prove.</b> That the counts are right is
 * {@code DashboardSummaryServiceTest}'s question, and AC-miniloan-112's cross-aggregate trap — a
 * Disbursed application counted once as Active and never again under Approved — is answered there
 * against the database. Recomputing any of it here would be a second answer to a question already
 * settled, which is what gate 87 exists to stop.
 *
 * <p>What is measured here is the half a service test cannot see: that the page puts each of the
 * five numbers in the field mock named for it and does not cross two of them over, that it adds
 * nothing of its own, and — the one a house-style refactor would break — that a zero is rendered as
 * "0" rather than as the "—" every other screen in this app uses for a missing value.
 */
describe('DashboardComponent (UI-miniloan-009)', () => {
  let httpTesting: HttpTestingController;

  const URL = `${API_BASE_URL}/dashboard`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** AC-miniloan-110's numbers, exactly as the criterion states them. */
  const AC110: DashboardSummary = {
    submittedApplications: 3,
    underReviewApplications: 2,
    approvedApplications: 1,
    activeLoanAccounts: 5,
    closedLoanAccounts: 4,
  };

  /** AC-miniloan-111 — the lower boundary of counting. */
  const EMPTY: DashboardSummary = {
    submittedApplications: 0,
    underReviewApplications: 0,
    approvedApplications: 0,
    activeLoanAccounts: 0,
    closedLoanAccounts: 0,
  };

  /**
   * AC-miniloan-112 — one Disbursed application with one Active account. The API has already decided
   * it is Active 1 and Approved 0; this fixture is that decision, not a recomputation of it.
   */
  const AC112: DashboardSummary = {
    submittedApplications: 0,
    underReviewApplications: 0,
    approvedApplications: 0,
    activeLoanAccounts: 1,
    closedLoanAccounts: 0,
  };

  function render(summary: DashboardSummary = AC110) {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      httpTesting.expectOne(URL).flush(summary);
      fixture.detectChanges();
      return fixture;
    });
  }

  const el = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  const text = (fixture: { nativeElement: unknown }, id: string) =>
    el(fixture, id)?.textContent?.trim();

  it('renders every control mock minted for this screen', async () => {
    const fixture = await render();

    for (const id of [
      'ui-miniloan-009-count-applications-submitted',
      'ui-miniloan-009-count-applications-under-review',
      'ui-miniloan-009-count-applications-approved',
      'ui-miniloan-009-count-accounts-active',
      'ui-miniloan-009-count-accounts-closed',
      'ui-miniloan-009-refresh',
    ]) {
      expect(el(fixture, id)).not.toBeNull();
    }
  });

  /**
   * AC-miniloan-110, all five and each in its own field. The values are deliberately all different,
   * so a template that crossed two of them over — Approved into Active, say — cannot pass; a fixture
   * of five equal numbers would hide exactly that mistake.
   */
  it('shows AC-miniloan-110 exactly: 3 · 2 · 1 · 5 · 4, each in its own field', async () => {
    const fixture = await render(AC110);

    expect(text(fixture, 'ui-miniloan-009-count-applications-submitted')).toBe('3');
    expect(text(fixture, 'ui-miniloan-009-count-applications-under-review')).toBe('2');
    expect(text(fixture, 'ui-miniloan-009-count-applications-approved')).toBe('1');
    expect(text(fixture, 'ui-miniloan-009-count-accounts-active')).toBe('5');
    expect(text(fixture, 'ui-miniloan-009-count-accounts-closed')).toBe('4');
  });

  /**
   * AC-miniloan-111 — "เป็นเลขศูนย์ ไม่ใช่ช่องว่าง ไม่ใช่ขีด และไม่ใช่ข้อความว่าไม่มีข้อมูล". Every
   * other list screen in this app renders a missing value as "—", so this is the assertion that stops
   * a tidy-up from applying the house style here and quietly breaking the criterion.
   */
  it('renders an empty system as five zeros, not dashes or an empty-state message', async () => {
    const fixture = await render(EMPTY);
    const root = fixture.nativeElement as HTMLElement;

    for (const id of [
      'ui-miniloan-009-count-applications-submitted',
      'ui-miniloan-009-count-applications-under-review',
      'ui-miniloan-009-count-applications-approved',
      'ui-miniloan-009-count-accounts-active',
      'ui-miniloan-009-count-accounts-closed',
    ]) {
      expect(text(fixture, id)).toBe('0');
    }
    expect(root.textContent).not.toContain('—');
    expect(root.textContent).not.toContain('ไม่มีข้อมูล');
    expect(fixture.componentInstance.failed()).toBe(false);
  });

  /**
   * AC-miniloan-112's web half. The API sent Active 1 and Approved 0, and the page prints both as
   * they arrived — it does not notice that a Disbursed application "ought" to appear somewhere and
   * add it. The Approved cell being 0 while Active is 1 is the whole point of the criterion.
   */
  it('prints the cross-aggregate answer the API gave, without topping up Approved', async () => {
    const fixture = await render(AC112);

    expect(text(fixture, 'ui-miniloan-009-count-accounts-active')).toBe('1');
    expect(text(fixture, 'ui-miniloan-009-count-applications-approved')).toBe('0');
  });

  /**
   * screens.json's refresh action — kind: view, destinationRef: self. One press, one GET, and the
   * new numbers replace the old ones. httpTesting.verify() in afterEach is what proves there is no
   * polling behind it: a timer would show up as an unexpected request.
   */
  it('re-reads on refresh and shows the newer numbers', async () => {
    const fixture = await render(AC110);

    (el(fixture, 'ui-miniloan-009-refresh') as HTMLButtonElement).click();
    const again = httpTesting.expectOne(URL);
    expect(again.request.method).toBe('GET');
    again.flush({ ...AC110, submittedApplications: 7 });
    fixture.detectChanges();

    expect(text(fixture, 'ui-miniloan-009-count-applications-submitted')).toBe('7');
    expect(text(fixture, 'ui-miniloan-009-count-accounts-closed')).toBe('4');
  });

  /**
   * screens.json error. The refusal the API can still give — DASHBOARD_NOT_PERMITTED — lands here
   * rather than in an unauthorized branch, because screens.json declares that state "ไม่เกิด": nav
   * links /dashboard to ROLE-002 alone. No number of anybody's is left on screen.
   */
  it('offers a retry and shows no number at all when the read fails', async () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(URL).flush(
      { code: 'DASHBOARD_NOT_PERMITTED', message: 'ไม่มีสิทธิ์ดูแดชบอร์ดภาพรวมสถานะ' },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect(fixture.componentInstance.summary()).toBeNull();
    expect(el(fixture, 'ui-miniloan-009-count-accounts-active')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดแดชบอร์ดไม่สำเร็จ');

    // The retry is the same declared action, and it really re-reads.
    (el(fixture, 'ui-miniloan-009-refresh') as HTMLButtonElement).click();
    httpTesting.expectOne(URL).flush(AC110);
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(false);
    expect(text(fixture, 'ui-miniloan-009-count-accounts-active')).toBe('5');
  });
});

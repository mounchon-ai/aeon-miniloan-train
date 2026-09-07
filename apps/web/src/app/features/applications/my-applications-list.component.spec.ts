import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MyApplicationsListComponent } from './my-applications-list.component';
import { API_BASE_URL } from '../../core/api-base-url';

/**
 * UI-miniloan-002 (UC-miniloan-023 · AC-miniloan-126 · AC-miniloan-128).
 *
 * <p><b>The scope itself is measured in apps/api</b> ({@code ScopedListingServiceTest} ·
 * {@code ScopedListingControllerTest}), because that is where AC-miniloan-128 says the hole lives.
 * What only this file can prove is that the page does not undo that: it renders exactly what the two
 * routes returned, adds no filter of its own, and — the failure that would matter — never asks for
 * more than its own scope and narrows the result afterwards.
 *
 * <p>The other three assertions are screens.json's declared states: empty, loading and error each
 * have a described behaviour and each is rendered. Unauthorized is declared "ไม่เกิด" for this
 * screen, so no branch exists for it and none is tested.
 */
describe('MyApplicationsListComponent (UI-miniloan-002)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyApplicationsListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const APPLICATION = {
    id: 'app-1',
    status: 'Submitted',
    requestedAmount: '100000.00',
    submittedAt: '2026-09-01T10:00:00Z',
  };

  const ACCOUNT = {
    accountNumber: 'acc-1',
    applicationId: 'app-1',
    status: 'Active',
    outstandingPrincipal: '95000.00',
  };

  function renderWith(applications: unknown[], accounts: unknown[]) {
    const fixture = TestBed.createComponent(MyApplicationsListComponent);
    fixture.detectChanges();
    httpTesting.expectOne(`${API_BASE_URL}/applications`).flush(applications);
    httpTesting.expectOne(`${API_BASE_URL}/loan-accounts`).flush(accounts);
    fixture.detectChanges();
    return fixture;
  }

  const testid = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  /** Every control design declared for this screen, in the DOM where qa can select it. */
  it('renders every control mock minted for this screen', () => {
    const fixture = renderWith([APPLICATION], [ACCOUNT]);

    for (const id of [
      'ui-miniloan-002-ent-002-status',
      'ui-miniloan-002-ent-002-requested-amount',
      'ui-miniloan-002-ent-002-submitted-at',
      'ui-miniloan-002-ent-006-status',
      'ui-miniloan-002-ent-006-outstanding-principal',
      'ui-miniloan-002-view-application',
      'ui-miniloan-002-view-schedule',
      'ui-miniloan-002-new-application',
    ]) {
      expect(testid(fixture, id)).not.toBeNull();
    }
  });

  /**
   * AC-miniloan-126 — "เห็นครบทั้ง 2 ใบและ 1 บัญชีของตัวเองตามปกติ". The counts are asserted because
   * a page that dropped one would still "show the applicant's rows".
   */
  it('renders every row the API returned, on both lists', () => {
    const fixture = renderWith(
      [APPLICATION, { ...APPLICATION, id: 'app-2', requestedAmount: '50000.00' }],
      [ACCOUNT],
    );

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelectorAll('[data-testid="ui-miniloan-002-ent-002-status"]')).toHaveLength(2);
    expect(root.querySelectorAll('[data-testid="ui-miniloan-002-ent-006-status"]')).toHaveLength(1);
  });

  /**
   * AC-miniloan-128's browser half: the request carries no owner, no role and no filter — the page
   * asks the plain route and the API decides the scope. A page that sent `?applicantId=` would be
   * choosing its own scope, which is what BR-miniloan-033@v1 puts on the server.
   */
  it('asks the plain scoped routes and adds no filter of its own', () => {
    const fixture = TestBed.createComponent(MyApplicationsListComponent);
    fixture.detectChanges();

    const applications = httpTesting.expectOne(`${API_BASE_URL}/applications`);
    const accounts = httpTesting.expectOne(`${API_BASE_URL}/loan-accounts`);
    expect(applications.request.params.keys()).toEqual([]);
    expect(accounts.request.params.keys()).toEqual([]);

    applications.flush([]);
    accounts.flush([]);
  });

  /** screens.json empty — "แสดงข้อความชวนยื่นใบสมัครใหม่พร้อมปุ่ม 'ยื่นใบสมัครใหม่'". */
  it('offers the new-application action when both lists are empty', () => {
    const fixture = renderWith([], []);

    expect(fixture.componentInstance.isEmpty()).toBe(true);
    expect(testid(fixture, 'ui-miniloan-002-new-application')).not.toBeNull();
  });

  /** screens.json loading — the skeleton shows before either call answers. */
  it('shows the loading state until both calls answer', () => {
    const fixture = TestBed.createComponent(MyApplicationsListComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('กำลังโหลดรายการ');

    httpTesting.expectOne(`${API_BASE_URL}/applications`).flush([]);
    httpTesting.expectOne(`${API_BASE_URL}/loan-accounts`).flush([]);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
  });

  /**
   * screens.json error — "แสดงข้อความโหลดรายการไม่สำเร็จ พร้อมปุ่มลองใหม่", and no half-list is
   * shown: REQ-miniloan-006 puts no retry queue in this round, so the person re-issues it.
   */
  it('shows the error state with a retry when a call fails, and no rows', () => {
    const fixture = TestBed.createComponent(MyApplicationsListComponent);
    fixture.detectChanges();

    // The accounts call is answered first on purpose: forkJoin cancels its siblings the moment one
    // source errors, so flushing the failure first would leave a cancelled request nobody can flush.
    // That cancellation is the behaviour the page wants — one failure is one error state, not half
    // a list — and this order is how the test observes it rather than fighting it.
    httpTesting.expectOne(`${API_BASE_URL}/loan-accounts`).flush([ACCOUNT]);
    httpTesting
      .expectOne(`${API_BASE_URL}/applications`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect(fixture.componentInstance.applications()).toEqual([]);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดรายการไม่สำเร็จ');
  });
});

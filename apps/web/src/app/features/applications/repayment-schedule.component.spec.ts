import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { RepaymentScheduleComponent } from './repayment-schedule.component';
import { API_BASE_URL } from '../../core/api-base-url';

/**
 * UI-miniloan-004 (UC-miniloan-011 · AC-miniloan-098 · AC-miniloan-099).
 *
 * <p><b>What only a web test can prove.</b> That the API returns every instalment is
 * {@code RepaymentScheduleQueryServiceTest}'s question and it is answered there. What is measured
 * here is the half a service test cannot see: that the page renders all of them and drops none —
 * a filter on status or due date in the template would satisfy every API test and still break
 * AC-miniloan-098 — and that a 403 puts the API's own sentence on screen with no row of anyone
 * else's beside it.
 */
describe('RepaymentScheduleComponent (UI-miniloan-004)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RepaymentScheduleComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        // The payoff link points at UI-miniloan-005 (screens.json destinationRef), so the test
        // router needs that path to exist for routerLink to resolve.
        provideRouter([{ path: 'loan-accounts/:id/payoff-quote', children: [] }]),
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** One Paid row and two Due ones — AC-miniloan-098's "จ่ายแล้ว" and "ค้าง" side by side. */
  const SCHEDULE = {
    accountNumber: 'acc-1',
    accountStatus: 'Active',
    revisionNumber: 1,
    issuedAt: '2026-01-15T00:00:00Z',
    principalAmount: '100000.00',
    outstandingPrincipal: '92000.00',
    termMonths: 3,
    totalPrincipal: '100000.00',
    installments: [
      {
        number: 1,
        dueDate: '2026-02-15',
        emiAmount: '9497.03',
        interestPortion: '2083.33',
        principalPortion: '7413.70',
        remainingBalance: '92586.30',
        status: 'Paid',
      },
      {
        number: 2,
        dueDate: '2026-03-15',
        emiAmount: '9497.03',
        interestPortion: '1928.88',
        principalPortion: '7568.15',
        remainingBalance: '85018.15',
        status: 'Due',
      },
      {
        number: 3,
        dueDate: '2026-04-15',
        emiAmount: '9497.03',
        interestPortion: '1771.21',
        principalPortion: '7725.82',
        remainingBalance: '77292.33',
        status: 'Due',
      },
    ],
  };

  function render(id = 'acc-1') {
    const fixture = TestBed.createComponent(RepaymentScheduleComponent);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    return fixture;
  }

  const all = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelectorAll(`[data-testid="${id}"]`);

  it('renders every control mock minted for this screen', async () => {
    const fixture = render();
    await fixture.whenStable();
    httpTesting.expectOne(`${API_BASE_URL}/loan-accounts/acc-1/schedule`).flush(SCHEDULE);
    fixture.detectChanges();

    for (const id of [
      'ui-miniloan-004-ent-008-installment-number',
      'ui-miniloan-004-ent-008-due-date',
      'ui-miniloan-004-ent-008-emi-amount',
      'ui-miniloan-004-ent-008-interest-portion',
      'ui-miniloan-004-ent-008-principal-portion',
      'ui-miniloan-004-ent-008-remaining-balance',
      'ui-miniloan-004-ent-008-status',
      'ui-miniloan-004-ent-007-total-principal',
      'ui-miniloan-004-request-payoff-quote',
    ]) {
      expect(all(fixture, id).length).toBeGreaterThan(0);
    }
  });

  /**
   * AC-miniloan-098 — "ทุกงวดตั้งแต่งวดที่ 1 ถึงงวดสุดท้าย ไม่ใช่เฉพาะงวดที่ยังไม่ถึงกำหนด". The
   * count is asserted, and the paid row is asserted present by name: a template that filtered on
   * status would still show "the borrower's instalments" and would pass a weaker check.
   */
  it('renders every instalment the API sent, paid ones included, in order', async () => {
    const fixture = render();
    await fixture.whenStable();
    httpTesting.expectOne(`${API_BASE_URL}/loan-accounts/acc-1/schedule`).flush(SCHEDULE);
    fixture.detectChanges();

    const numbers = [...all(fixture, 'ui-miniloan-004-ent-008-installment-number')].map((cell) =>
      cell.textContent?.trim(),
    );
    expect(numbers).toEqual(['1', '2', '3']);

    const statuses = [...all(fixture, 'ui-miniloan-004-ent-008-status')].map((cell) =>
      cell.textContent?.trim(),
    );
    expect(statuses).toEqual(['Paid', 'Due', 'Due']);
  });

  /** "เห็นเงินต้น ดอกเบี้ย และยอดคงเหลือของแต่ละงวด" — all three on every row, as the API sent them. */
  it('renders the interest, principal and balance of each row unchanged', async () => {
    const fixture = render();
    await fixture.whenStable();
    httpTesting.expectOne(`${API_BASE_URL}/loan-accounts/acc-1/schedule`).flush(SCHEDULE);
    fixture.detectChanges();

    expect(all(fixture, 'ui-miniloan-004-ent-008-interest-portion')[0].textContent).toContain(
      '2083.33',
    );
    expect(all(fixture, 'ui-miniloan-004-ent-008-principal-portion')[0].textContent).toContain(
      '7413.70',
    );
    expect(all(fixture, 'ui-miniloan-004-ent-008-remaining-balance')[0].textContent).toContain(
      '92586.30',
    );
  });

  /**
   * ENT-007's totalPrincipal is the API's figure in the schedule-summary zone. Nothing here sums the
   * instalments to reach it — the sum of this fixture's principal portions is not 100,000.00, and the
   * page must still show what the API said.
   */
  it('shows the schedule total the API sent rather than a sum of the rows', async () => {
    const fixture = render();
    await fixture.whenStable();
    httpTesting.expectOne(`${API_BASE_URL}/loan-accounts/acc-1/schedule`).flush(SCHEDULE);
    fixture.detectChanges();

    expect(all(fixture, 'ui-miniloan-004-ent-007-total-principal')[0].textContent).toContain(
      '100000.00',
    );
  });

  /**
   * AC-miniloan-099 — "ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้ … ไม่มีข้อมูลงวดใดของ ข. หลุดออกมาแม้แต่
   * แถวเดียว". The sentence asserted is this screen's own; UI-miniloan-005 gets a different one for
   * the same rule, and a refactor that unified them would change what a user reads.
   */
  it('shows the API refusal and no instalment row when the account is not the caller"s', async () => {
    const fixture = render('someone-elses-account');
    await fixture.whenStable();
    httpTesting
      .expectOne(`${API_BASE_URL}/loan-accounts/someone-elses-account/schedule`)
      .flush(
        { code: 'SCHEDULE_VIEW_APPLICANT_ONLY', message: 'ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้' },
        { status: 403, statusText: 'Forbidden' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.notAllowed()).toBe('ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้');
    expect(all(fixture, 'ui-miniloan-004-ent-008-installment-number')).toHaveLength(0);
    expect(fixture.componentInstance.schedule()).toBeNull();
  });

  /** screens.json gives unauthorized and error different behaviour; only error offers a retry. */
  it('separates a refusal from a failed load', async () => {
    const fixture = render();
    await fixture.whenStable();
    httpTesting
      .expectOne(`${API_BASE_URL}/loan-accounts/acc-1/schedule`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect(fixture.componentInstance.notAllowed()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดตารางผ่อนไม่สำเร็จ');
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AccountDetailComponent } from './account-detail.component';
import { API_BASE_URL } from '../../core/api-base-url';
import {
  Installment,
  LoanAccountSummary,
  RepaymentSchedule,
} from '../../core/services/loan-account.service';

/**
 * UI-miniloan-012 (AC-miniloan-004 · 005 · 006 · 007 · 008 · 009 · 010 · 011 · 107 · 108 · 109).
 *
 * <p><b>What only a web test can prove.</b> That a Closed account refuses a fresh revision is
 * BR-miniloan-045@v1's question and AmortizationScheduleReissueTest answers it; that a row of the
 * revision in force cannot be edited is measured over HTTP in RepaymentScheduleControllerTest. What
 * is measured here is the half those cannot see: that this screen mints no control design did not
 * declare — no "ปิดบัญชี" button and no "ดูฉบับก่อนหน้า" link — that the closing sentence follows
 * the API's closeReason rather than a guess, that instalment states are labelled in the criteria's
 * own words, and that the schedule zone can fail on its own without taking the account with it.
 */
describe('AccountDetailComponent (UI-miniloan-012)', () => {
  let httpTesting: HttpTestingController;

  const ID = 'acc-1';
  const ACCOUNT_URL = `${API_BASE_URL}/loan-accounts/${ID}`;
  const SCHEDULE_URL = `${API_BASE_URL}/loan-accounts/${ID}/schedule`;
  const RESCHEDULE_URL = `${API_BASE_URL}/loan-accounts/${ID}/reschedule`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountDetailComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const account = (over: Partial<LoanAccountSummary> = {}): LoanAccountSummary => ({
    accountNumber: ID,
    applicationId: 'app-1',
    status: 'Active',
    principalAmount: '100000.00',
    outstandingPrincipal: '70000.00',
    termMonths: 12,
    disbursedAt: '2026-01-01T00:00:00Z',
    closedAt: null,
    closeReason: null,
    nextDueInstallmentNumber: 3,
    ...over,
  });

  const installment = (number: number, status: string): Installment => ({
    number,
    dueDate: `2026-0${((number - 1) % 9) + 1}-01`,
    emiAmount: '9583.33',
    interestPortion: '2083.33',
    principalPortion: '7500.00',
    remainingBalance: '70000.00',
    status,
  });

  const schedule = (over: Partial<RepaymentSchedule> = {}): RepaymentSchedule => ({
    accountNumber: ID,
    accountStatus: 'Active',
    revisionNumber: 1,
    issuedAt: '2026-01-01T00:00:00Z',
    principalAmount: '100000.00',
    outstandingPrincipal: '70000.00',
    termMonths: 12,
    totalPrincipal: '100000.00',
    installments: [
      installment(1, 'Paid'),
      installment(2, 'Paid'),
      installment(3, 'Due'),
      installment(4, 'Due'),
    ],
    ...over,
  });

  async function render(
    summary: LoanAccountSummary = account(),
    table: RepaymentSchedule | null = schedule(),
  ) {
    const fixture = TestBed.createComponent(AccountDetailComponent);
    fixture.componentRef.setInput('id', ID);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(ACCOUNT_URL).flush(summary);
    fixture.detectChanges();

    const scheduleRequest = httpTesting.expectOne(SCHEDULE_URL);
    if (table) {
      scheduleRequest.flush(table);
    } else {
      scheduleRequest.flush(
        { code: 'LOAN_ACCOUNT_NOT_ACTIVE', message: 'ดูตารางผ่อนไม่ได้ — บัญชีนี้ปิดแล้ว' },
        { status: 409, statusText: 'Conflict' },
      );
    }
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  const cells = (fixture: { nativeElement: unknown }, id: string) =>
    [...(fixture.nativeElement as HTMLElement).querySelectorAll(`[data-testid="${id}"]`)].map(
      (cell) => cell.textContent?.trim(),
    );

  const text = (fixture: { nativeElement: unknown }) =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('renders every control mock minted for the two zones this component owns', async () => {
    const fixture = await render();

    for (const id of [
      'ui-miniloan-012-ent-006-principal-amount',
      'ui-miniloan-012-ent-006-outstanding-principal',
      'ui-miniloan-012-ent-006-term-months',
      'ui-miniloan-012-ent-006-status',
      'ui-miniloan-012-ent-006-disbursed-at',
      'ui-miniloan-012-ent-006-closed-at',
      'ui-miniloan-012-ent-008-installment-number',
      'ui-miniloan-012-ent-008-due-date',
      'ui-miniloan-012-ent-008-emi-amount',
      'ui-miniloan-012-ent-008-status',
      'ui-miniloan-012-reschedule',
    ]) {
      expect(el(fixture, id)).not.toBeNull();
    }
  });

  /** The payment zone is on the page, and it is handed the instalment ACL-011's condition names. */
  it('hosts the payment form and gives it the earliest Due instalment', async () => {
    const fixture = await render();

    expect(el(fixture, 'ui-miniloan-012-ent-009-amount')).not.toBeNull();
    expect(el(fixture, 'ui-miniloan-012-record-payment')).not.toBeNull();
    expect(fixture.componentInstance.nextDueInstallment()).toBe(3);
  });

  /** ACL-015's condition: the adjustment form belongs to a Closed account and to no other. */
  it('shows the adjustment form only once the account is Closed', async () => {
    const active = await render();
    expect(el(active, 'ui-miniloan-012-submit-adjustment')).toBeNull();

    httpTesting.verify();

    const closed = await render(
      account({ status: 'Closed', closedAt: '2026-05-01T00:00:00Z', closeReason: 'FullyPaid' }),
      null,
    );
    expect(el(closed, 'ui-miniloan-012-ent-010-target-record-id')).not.toBeNull();
    expect(el(closed, 'ui-miniloan-012-submit-adjustment')).not.toBeNull();
  });

  /** STM-miniloan-003's three states in the words AC-miniloan-004 and AC-miniloan-005 use. */
  it('labels each instalment in the criteria own words', async () => {
    const fixture = await render(
      account(),
      schedule({
        installments: [
          installment(1, 'Paid'),
          installment(2, 'Due'),
          installment(3, 'Cancelled'),
        ],
      }),
    );

    expect(cells(fixture, 'ui-miniloan-012-ent-008-status')).toEqual([
      'จ่ายแล้ว',
      'ค้าง',
      'ยกเลิก (ปิดบัญชีก่อนกำหนด)',
    ]);
    expect(cells(fixture, 'ui-miniloan-012-ent-008-installment-number')).toEqual(['1', '2', '3']);
  });

  /**
   * AC-miniloan-004 and AC-miniloan-005 word the closing differently, and the difference is
   * closeReason — a fact about the account. A page that used one sentence for both would tell an
   * early settlement it had paid every instalment.
   */
  it('shows AC-miniloan-004 for a fully-paid close', async () => {
    const fixture = await render(
      account({ status: 'Closed', closedAt: '2026-05-01T00:00:00Z', closeReason: 'FullyPaid' }),
      null,
    );

    expect(text(fixture)).toContain('ปิดบัญชีแล้ว (Closed)');
    expect(fixture.componentInstance.closingSentence()).toBe(
      'ชำระครบทุกงวดแล้ว — ปิดบัญชีเมื่อ 2026-05-01T00:00:00Z',
    );
  });

  it('shows AC-miniloan-005 for an early settlement, not the same sentence', async () => {
    const fixture = await render(
      account({
        status: 'Closed',
        closedAt: '2026-05-01T00:00:00Z',
        closeReason: 'EarlySettlement',
      }),
      null,
    );

    expect(fixture.componentInstance.closingSentence()).toBe(
      'ปิดบัญชีก่อนกำหนด — ชำระยอดปิดบัญชีครบเมื่อ 2026-05-01T00:00:00Z',
    );
  });

  /** An Active account gets neither sentence — the closing line is not a permanent fixture. */
  it('shows no closing sentence while the account is Active', async () => {
    const fixture = await render();

    expect(fixture.componentInstance.closingSentence()).toBeNull();
    expect(text(fixture)).toContain('กำลังผ่อนชำระ (Active)');
    expect(text(fixture)).not.toContain('ปิดบัญชีเมื่อ');
  });

  /** AC-miniloan-008's first half: exactly one revision is ever marked as in force. */
  it('names the revision in force and when it was issued', async () => {
    const fixture = await render(account(), schedule({ revisionNumber: 2 }));

    expect(text(fixture)).toContain('ฉบับที่ 2 (ใช้อยู่) — ออกเมื่อ 2026-01-01T00:00:00Z');
  });

  /**
   * AC-miniloan-008 · AC-miniloan-011's revision list, from API-011's own revisions[] — every
   * revision named, exactly one marked "ใช้อยู่", each superseded one carrying the date it was
   * replaced. Opening a superseded revision's ROWS has neither an endpoint nor a minted control and
   * is carried up as a gap card instead of being invented here.
   */
  it('lists every revision after a reschedule, with one and only one in force', async () => {
    const fixture = await render();

    (el(fixture, 'ui-miniloan-012-reschedule') as HTMLButtonElement).click();
    const request = httpTesting.expectOne(RESCHEDULE_URL);
    expect(request.request.method).toBe('POST');
    request.flush({
      accountNumber: ID,
      accountStatus: 'Active',
      revisionNumber: 3,
      issuedAt: '2026-04-01T00:00:00Z',
      totalPrincipal: '80000.00',
      revisions: [
        {
          revisionNumber: 1,
          issuedAt: '2026-01-01T00:00:00Z',
          supersededAt: '2026-02-01T00:00:00Z',
          current: false,
          totalPrincipal: '100000.00',
        },
        {
          revisionNumber: 2,
          issuedAt: '2026-02-01T00:00:00Z',
          supersededAt: '2026-04-01T00:00:00Z',
          current: false,
          totalPrincipal: '90000.00',
        },
        {
          revisionNumber: 3,
          issuedAt: '2026-04-01T00:00:00Z',
          supersededAt: null,
          current: true,
          totalPrincipal: '80000.00',
        },
      ],
      installments: [installment(4, 'Due')],
    });
    fixture.detectChanges();

    // the reschedule re-reads the account and its schedule
    httpTesting.expectOne(ACCOUNT_URL).flush(account());
    fixture.detectChanges();
    httpTesting.expectOne(SCHEDULE_URL).flush(schedule({ revisionNumber: 3 }));
    fixture.detectChanges();

    const listed = [...(fixture.nativeElement as HTMLElement).querySelectorAll('li')].map((item) =>
      item.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(listed).toEqual([
      'ฉบับที่ 1 — ถูกแทนที่เมื่อ 2026-02-01T00:00:00Z',
      'ฉบับที่ 2 — ถูกแทนที่เมื่อ 2026-04-01T00:00:00Z',
      'ฉบับที่ 3 (ใช้อยู่)',
    ]);
    expect(listed.filter((line) => line?.includes('(ใช้อยู่)'))).toHaveLength(1);
  });

  /**
   * AC-miniloan-107 · AC-miniloan-108 · AC-miniloan-109. The button is NOT hidden when the account
   * closes — AC-miniloan-109's point is that the permission goes away with the closing, and
   * BR-miniloan-025@v1 requires that to be enforced where the rule lives. The refusal is the API's
   * sentence, rendered untouched.
   */
  it('lets a Closed account be asked, and renders the API refusal word for word', async () => {
    const fixture = await render(
      account({ status: 'Closed', closedAt: '2026-05-01T00:00:00Z', closeReason: 'FullyPaid' }),
      null,
    );

    const button = el(fixture, 'ui-miniloan-012-reschedule') as HTMLButtonElement;
    expect(button.disabled).toBe(false);

    button.click();
    httpTesting.expectOne(RESCHEDULE_URL).flush(
      {
        code: 'LOAN_ACCOUNT_CLOSED',
        message: 'บัญชีนี้ปิดแล้ว — ออกตารางผ่อนฉบับใหม่ทับไม่ได้',
      },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.rescheduleFailed()).toBe(
      'บัญชีนี้ปิดแล้ว — ออกตารางผ่อนฉบับใหม่ทับไม่ได้',
    );
  });

  /**
   * AC-miniloan-006 · AC-miniloan-069 speak of a "ปิดบัญชี" button; AC-miniloan-008 of a
   * "ดูฉบับก่อนหน้า" control. screens.json declares four actions on this screen and mock minted four
   * ids, and neither of those is among them — so this asserts the absence rather than minting an id
   * gate 37 would never let anybody change. Both are carried up as gap cards.
   */
  it('mints no control design did not declare', async () => {
    const fixture = await render();
    const root = fixture.nativeElement as HTMLElement;

    expect([...root.querySelectorAll('[data-testid]')].map((node) => node.getAttribute('data-testid')))
      .toEqual([
        'ui-miniloan-012-ent-006-principal-amount',
        'ui-miniloan-012-ent-006-outstanding-principal',
        'ui-miniloan-012-ent-006-term-months',
        'ui-miniloan-012-ent-006-status',
        'ui-miniloan-012-ent-006-disbursed-at',
        'ui-miniloan-012-ent-006-closed-at',
        'ui-miniloan-012-reschedule',
        'ui-miniloan-012-ent-008-installment-number',
        'ui-miniloan-012-ent-008-due-date',
        'ui-miniloan-012-ent-008-emi-amount',
        'ui-miniloan-012-ent-008-status',
        'ui-miniloan-012-ent-008-installment-number',
        'ui-miniloan-012-ent-008-due-date',
        'ui-miniloan-012-ent-008-emi-amount',
        'ui-miniloan-012-ent-008-status',
        'ui-miniloan-012-ent-008-installment-number',
        'ui-miniloan-012-ent-008-due-date',
        'ui-miniloan-012-ent-008-emi-amount',
        'ui-miniloan-012-ent-008-status',
        'ui-miniloan-012-ent-008-installment-number',
        'ui-miniloan-012-ent-008-due-date',
        'ui-miniloan-012-ent-008-emi-amount',
        'ui-miniloan-012-ent-008-status',
        'ui-miniloan-012-ent-009-amount',
        'ui-miniloan-012-record-payment',
        'ui-miniloan-012-record-payoff-payment',
      ]);
  });

  /**
   * screens.json unauthorized — "ไม่มีสิทธิ์ดำเนินการกับบัญชีนี้ … BR-miniloan-054@v1". The API's own
   * sentence, and not one figure of the account is left on screen.
   */
  it('shows the API refusal and no account data when the account is not visible', async () => {
    const fixture = TestBed.createComponent(AccountDetailComponent);
    fixture.componentRef.setInput('id', ID);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(ACCOUNT_URL).flush(
      { code: 'LOAN_ACCOUNT_NOT_VISIBLE', message: 'ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้' },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.notAllowed()).toBe('ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้');
    expect(el(fixture, 'ui-miniloan-012-ent-006-outstanding-principal')).toBeNull();
    // and the schedule is never asked for — verify() in afterEach is what proves it
  });

  /**
   * The schedule zone fails on its own. A Closed account's table is readable by the assigned
   * Operations person (ACL-020 declares no state condition), but any other refusal must not take the
   * account summary or the adjustment form down with it.
   */
  it('keeps the account on screen when only the schedule read fails', async () => {
    const fixture = await render(
      account({ status: 'Closed', closedAt: '2026-05-01T00:00:00Z', closeReason: 'FullyPaid' }),
      null,
    );

    expect(fixture.componentInstance.scheduleFailed()).toBe('ดูตารางผ่อนไม่ได้ — บัญชีนี้ปิดแล้ว');
    expect(el(fixture, 'ui-miniloan-012-ent-006-principal-amount')?.textContent?.trim()).toBe(
      '100000.00',
    );
    expect(el(fixture, 'ui-miniloan-012-submit-adjustment')).not.toBeNull();
    expect(fixture.componentInstance.nextDueInstallment()).toBeNull();
  });
});

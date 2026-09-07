import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { RecordPaymentComponent } from './record-payment.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { PaymentResult } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-012's payment-form zone (AC-miniloan-012 · 013 · 014 · 069 · 070 · 086 · 087 · 088).
 *
 * <p><b>What only a web test can prove.</b> Whether 0.01 short is refused and whether 0.01 over is
 * an overpayment are BR-miniloan-019@v1's and BR-miniloan-046@v2's questions, and PaymentServiceTest
 * already answers both against the database with the golden figures. Recomputing any of that here
 * would be a second answer to a settled question (gate 87).
 *
 * <p>What is measured here is the half no service test can see: that the request carries the
 * instalment ACL-011's condition names rather than one this page chose, that the sentence
 * AC-miniloan-086 quotes is assembled from the right three figures — the point of that criterion
 * being that the principal fell by 19,800.00 and not by the 20,000.00 handed over — and that every
 * refusal reaches the screen in the API's own words rather than being reworded on the way.
 */
describe('RecordPaymentComponent (UI-miniloan-012 · payment-form)', () => {
  let httpTesting: HttpTestingController;

  const ACCOUNT = 'acc-1';
  const PAYMENTS = `${API_BASE_URL}/loan-accounts/${ACCOUNT}/payments`;
  const SETTLEMENT = `${API_BASE_URL}/loan-accounts/${ACCOUNT}/payoff-settlement`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecordPaymentComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const result = (over: Partial<PaymentResult>): PaymentResult => ({
    paymentId: 'pay-1',
    accountNumber: ACCOUNT,
    accountStatus: 'Active',
    closeReason: null,
    closedAt: null,
    installmentNumber: 3,
    installmentStatus: 'Paid',
    amount: '9583.33',
    paymentType: 'InstallmentExact',
    overpaymentAmount: null,
    prepaymentFee: null,
    principalReduction: null,
    outstandingPrincipal: '70000.00',
    reissuedRevisionNumber: null,
    ...over,
  });

  function render(nextDue: number | null = 3) {
    const fixture = TestBed.createComponent(RecordPaymentComponent);
    fixture.componentRef.setInput('accountId', ACCOUNT);
    fixture.componentRef.setInput('nextDueInstallment', nextDue);
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  const press = (fixture: { nativeElement: unknown }, id: string) =>
    (el(fixture, id) as HTMLButtonElement).click();

  const type = (fixture: { nativeElement: unknown }, value: string) => {
    const input = el(fixture, 'ui-miniloan-012-ent-009-amount') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  };

  const text = (fixture: { nativeElement: unknown }) =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('renders every control mock minted for this zone', () => {
    const fixture = render();

    for (const id of [
      'ui-miniloan-012-ent-009-amount',
      'ui-miniloan-012-record-payment',
      'ui-miniloan-012-record-payoff-payment',
    ]) {
      expect(el(fixture, id)).not.toBeNull();
    }
  });

  /**
   * ACL-011 and ACL-012 carry condition state ["Due"], and instalments are retired in order
   * (BR-miniloan-020@v1) — so the number sent is the earliest Due row of the schedule the parent
   * loaded. A page that sent 1 always, or the last row, would look identical on a fresh account.
   */
  it('sends the instalment the schedule says is owing, and the amount as typed', () => {
    const fixture = render(6);
    type(fixture, '9583.33');
    press(fixture, 'ui-miniloan-012-record-payment');

    const request = httpTesting.expectOne(PAYMENTS);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ installmentNumber: 6, amount: '9583.33' });
    request.flush(result({ installmentNumber: 6 }));
    fixture.detectChanges();
  });

  /**
   * The request carries no payment type, and PaymentRequest's own note says why: a caller able to
   * label its own payment an overpayment could label a short one that way too.
   */
  it('never tells the API what kind of payment this is', () => {
    const fixture = render();
    type(fixture, '20000.00');
    press(fixture, 'ui-miniloan-012-record-payment');

    const request = httpTesting.expectOne(PAYMENTS);
    expect(Object.keys(request.request.body as object).sort()).toEqual([
      'amount',
      'installmentNumber',
    ]);
    request.flush(result({}));
    fixture.detectChanges();
  });

  /** AC-miniloan-013, word for word. */
  it('shows AC-miniloan-013 exactly for an on-time payment', () => {
    const fixture = render();
    type(fixture, '9583.33');
    press(fixture, 'ui-miniloan-012-record-payment');
    httpTesting.expectOne(PAYMENTS).flush(result({ installmentNumber: 3 }));
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe('บันทึกการชำระงวดที่ 3 เรียบร้อย');
    expect(text(fixture)).toContain('บันทึกการชำระงวดที่ 3 เรียบร้อย');
  });

  /**
   * AC-miniloan-086's sentence, word for word — the sharpest assertion in this unit. All three
   * figures are different and one of them is a trap: a component that put overpaymentAmount in the
   * "ตัดเงินต้น" slot would print 20,000.00 there and would pass any check that only looked for the
   * numbers being present somewhere on the page.
   */
  it('assembles AC-miniloan-086 from the API figures, with 19,800.00 as the principal cut', () => {
    const fixture = render(6);
    type(fixture, '29583.33');
    press(fixture, 'ui-miniloan-012-record-payment');
    httpTesting.expectOne(PAYMENTS).flush(
      result({
        installmentNumber: 6,
        paymentType: 'InstallmentOverpayment',
        overpaymentAmount: '20000.00',
        prepaymentFee: '200.00',
        principalReduction: '19800.00',
        reissuedRevisionNumber: 2,
      }),
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe(
      'บันทึกการชำระงวดที่ 6 เรียบร้อย — ส่วนเกิน 20,000.00 บาท' +
        ' · ค่าธรรมเนียมการโปะ 200.00 บาท · ตัดเงินต้น 19,800.00 บาท',
    );
  });

  /**
   * AC-miniloan-087 — the boundary, and DQ-miniloan-001's decimal question, which closed at design's
   * desk at two places with round-half-up. The criterion's own hedge ("ยังไม่ถูกเคาะ") is therefore
   * stale: 0.00 is the settled answer, and it must print as 0.00 and not as 0.
   */
  it('shows AC-miniloan-087 with a 0.00 fee, at two decimal places', () => {
    const fixture = render(6);
    type(fixture, '9583.34');
    press(fixture, 'ui-miniloan-012-record-payment');
    httpTesting.expectOne(PAYMENTS).flush(
      result({
        installmentNumber: 6,
        paymentType: 'InstallmentOverpayment',
        overpaymentAmount: '0.01',
        prepaymentFee: '0.00',
        principalReduction: '0.01',
      }),
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe(
      'บันทึกการชำระงวดที่ 6 เรียบร้อย — ส่วนเกิน 0.01 บาท' +
        ' · ค่าธรรมเนียมการโปะ 0.00 บาท · ตัดเงินต้น 0.01 บาท',
    );
  });

  /**
   * AC-miniloan-012 and AC-miniloan-014 — the refusal is the API's sentence, rendered untouched, and
   * nothing about it is composed here. AC-miniloan-014's other half, that two half payments do not
   * accumulate, is the service's and is measured there.
   */
  it('renders the short-payment refusal in the API words and keeps the amount typed', () => {
    const fixture = render();
    type(fixture, '9583.32');
    press(fixture, 'ui-miniloan-012-record-payment');
    httpTesting.expectOne(PAYMENTS).flush(
      {
        code: 'INSTALLMENT_UNDERPAID',
        message:
          'ยอดชำระไม่ครบงวด — งวดที่ 3 ต้องชำระเต็มจำนวน 9,583.33 บาท ระบบไม่รับชำระบางส่วน',
      },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(
      'ยอดชำระไม่ครบงวด — งวดที่ 3 ต้องชำระเต็มจำนวน 9,583.33 บาท ระบบไม่รับชำระบางส่วน',
    );
    expect(fixture.componentInstance.outcome()).toBeNull();
    // screens.json: the value stays so a retry does not make somebody retype it.
    expect(fixture.componentInstance.amount.value).toBe('9583.32');
  });

  /** AC-miniloan-088 — a closed account refuses further payment, in the API's own words. */
  it('renders the closed-account refusal in the API words', () => {
    const fixture = render(null);
    type(fixture, '5000.00');
    press(fixture, 'ui-miniloan-012-record-payment');
    httpTesting.expectOne(PAYMENTS).flush(
      { code: 'LOAN_ACCOUNT_CLOSED', message: 'บัญชีนี้ปิดแล้ว — บันทึกการชำระเพิ่มไม่ได้' },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe('บัญชีนี้ปิดแล้ว — บันทึกการชำระเพิ่มไม่ได้');
  });

  /**
   * AC-miniloan-070's API half. The Applicant never reaches this screen — UI-miniloan-012 is
   * ROLE-004 and nav links it to nobody else — but a direct call is still refused there, and this
   * page renders that refusal rather than a sentence of its own.
   */
  it('renders the role refusal the API gives, never one of its own', () => {
    const fixture = render();
    type(fixture, '9583.33');
    press(fixture, 'ui-miniloan-012-record-payment');
    httpTesting.expectOne(PAYMENTS).flush(
      {
        code: 'PAYMENT_OPERATIONS_ONLY',
        message: 'ไม่มีสิทธิ์บันทึกการชำระ — การบันทึกการชำระทำได้เฉพาะเจ้าหน้าที่ Operations',
      },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(
      'ไม่มีสิทธิ์บันทึกการชำระ — การบันทึกการชำระทำได้เฉพาะเจ้าหน้าที่ Operations',
    );
  });

  /**
   * AC-miniloan-005's command. The second action posts to the settlement endpoint with the amount
   * typed and a closing date of today — the endpoint takes one, and the day the request is made is
   * not a decision the browser took. The closing SENTENCE is the parent's, from closeReason.
   */
  it('sends the payoff settlement with the amount typed and today as the closing date', () => {
    const fixture = render();
    type(fixture, '55000.00');
    press(fixture, 'ui-miniloan-012-record-payoff-payment');

    const request = httpTesting.expectOne(SETTLEMENT);
    expect(request.request.method).toBe('POST');
    const body = request.request.body as { closingDate: string; amount: string };
    expect(body.amount).toBe('55000.00');
    expect(body.closingDate).toBe(new Date().toISOString().slice(0, 10));

    request.flush({
      paymentId: 'pay-9',
      loanAccountId: ACCOUNT,
      status: 'Closed',
      closeReason: 'EarlySettlement',
      closedAt: '2026-06-01T00:00:00Z',
      amountPaid: '55000.00',
      earlySettlementFee: '1000.00',
      cancelledInstallments: [6, 7, 8, 9, 10, 11, 12],
    });
    fixture.detectChanges();

    // No second sentence about the closing is composed here — see the component's note.
    expect(fixture.componentInstance.outcome()).toBeNull();
  });

  /** screens.json loading — the pressed button is disabled while its own call is in flight. */
  it('disables both actions while one is in flight', () => {
    const fixture = render();
    type(fixture, '9583.33');
    press(fixture, 'ui-miniloan-012-record-payment');
    fixture.detectChanges();

    expect((el(fixture, 'ui-miniloan-012-record-payment') as HTMLButtonElement).disabled).toBe(true);
    expect(
      (el(fixture, 'ui-miniloan-012-record-payoff-payment') as HTMLButtonElement).disabled,
    ).toBe(true);

    httpTesting.expectOne(PAYMENTS).flush(result({}));
    fixture.detectChanges();

    expect((el(fixture, 'ui-miniloan-012-record-payment') as HTMLButtonElement).disabled).toBe(
      false,
    );
  });

  /**
   * AC-miniloan-006 · AC-miniloan-069 speak of a "ปิดบัญชี" button, and this zone has none: design
   * declares two actions here and mock minted two ids. Gate 37 forbids inventing a third, so the
   * absence is measured and carried up as a gap card. If somebody later adds the control without a
   * minted id, this fails.
   */
  it('mints no close-account control of its own', () => {
    const fixture = render();
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');

    expect([...buttons].map((button) => button.getAttribute('data-testid'))).toEqual([
      'ui-miniloan-012-record-payment',
      'ui-miniloan-012-record-payoff-payment',
    ]);
  });
});

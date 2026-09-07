import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { EarlyClosureQuoteComponent } from './early-closure-quote.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { PayoffQuote } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-005 (UC-miniloan-015 · AC-miniloan-071 · AC-miniloan-072 · AC-miniloan-073 ·
 * AC-miniloan-074 · AC-miniloan-075).
 *
 * <p><b>The figures below are GD-miniloan-005's rows, word for word</b> — C1, C2 and C3, the three
 * the criteria quote, signed by the spec owner on 2026-09-01 for CALC-miniloan-004@v1. Nothing is
 * computed here and nothing is re-derived: the API already proves it produces them
 * ({@code EarlyClosureQuoteServiceTest$GoldenRows}), and what this file measures is that the page
 * puts those exact numbers in front of the borrower. AC-miniloan-073's sentence is a line on a
 * screen — "ดอกเบี้ยค้างจ่าย 10 วัน … 342.47 บาท" — so the day count is asserted beside the amount.
 * C2's boundary is the one a page rendering only the amount would silently pass: 0 วัน — 0.00.
 *
 * <p><b>AC-miniloan-072 is measured as an absence.</b> The criterion has the applicant press
 * "ยืนยันปิดบัญชี" and be refused, but screens.json declares one action on this screen and mock
 * minted one action id, so a close button here would be a control nobody asked for carrying a testid
 * nobody minted. The API half is built and Operations-only. What is provable on the page is that no
 * such action exists and that visiting the screen issues no settlement call at all.
 */
describe('EarlyClosureQuoteComponent (UI-miniloan-005)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EarlyClosureQuoteComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'loan-accounts/:id/schedule', children: [] }]),
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** GD-miniloan-005 C1 — 10 วัน, the row AC-miniloan-073 quotes. */
  const C1 = {
    closingDate: '2026-06-25',
    remainingPrincipal: '50,000.00',
    daysElapsed: 10,
    accruedInterest: '342.47',
    earlySettlementFee: '500.00',
    earlySettlementAmount: '50,842.47',
  };

  /** GD-miniloan-005 C2 — 0 วัน, the lower boundary AC-miniloan-074 quotes. */
  const C2 = {
    closingDate: '2026-06-15',
    remainingPrincipal: '50,000.00',
    daysElapsed: 0,
    accruedInterest: '0.00',
    earlySettlementFee: '500.00',
    earlySettlementAmount: '50,500.00',
  };

  /** GD-miniloan-005 C3 — 1 วัน, the other side of that boundary (AC-miniloan-075). */
  const C3 = {
    closingDate: '2026-06-16',
    remainingPrincipal: '50,000.00',
    daysElapsed: 1,
    accruedInterest: '34.25',
    earlySettlementFee: '500.00',
    earlySettlementAmount: '50,534.25',
  };

  function renderWith(quote: PayoffQuote, id = 'acc-1') {
    const fixture = TestBed.createComponent(EarlyClosureQuoteComponent);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      const request = httpTesting.expectOne(
        (candidate) => candidate.url === `${API_BASE_URL}/loan-accounts/${id}/payoff-quote`,
      );
      request.flush(quote);
      fixture.detectChanges();
      return { fixture, request };
    });
  }

  const text = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement)
      .querySelector(`[data-testid="${id}"]`)
      ?.textContent?.trim();

  it('renders every control mock minted for this screen', async () => {
    const { fixture } = await renderWith(C1);

    for (const id of [
      'ui-miniloan-005-ent-006-outstanding-principal',
      'ui-miniloan-005-accrued-interest',
      'ui-miniloan-005-early-closure-fee',
      'ui-miniloan-005-total-payoff-amount',
      'ui-miniloan-005-quote-calculated-at',
      'ui-miniloan-005-back-to-schedule',
    ]) {
      expect(
        (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`),
      ).not.toBeNull();
    }
  });

  /** AC-miniloan-073 · GD-miniloan-005 C1 — the four figures and the day count, as signed. */
  it('shows GD-miniloan-005 C1 exactly: 10 วัน, 342.47, 500.00, total 50,842.47', async () => {
    const { fixture } = await renderWith(C1);

    expect(text(fixture, 'ui-miniloan-005-ent-006-outstanding-principal')).toBe('50,000.00');
    expect(text(fixture, 'ui-miniloan-005-accrued-interest')).toBe('10 วัน — 342.47');
    expect(text(fixture, 'ui-miniloan-005-early-closure-fee')).toBe('500.00');
    expect(text(fixture, 'ui-miniloan-005-total-payoff-amount')).toBe('50,842.47');
  });

  /**
   * AC-miniloan-074 · GD-miniloan-005 C2 — "ดอกเบี้ยค้างจ่าย 0 วัน — 0.00 บาท" and a total of
   * 50,500.00. The day count is asserted because a page that showed only "0.00" would look right
   * while hiding the reason it is zero.
   */
  it('shows GD-miniloan-005 C2 exactly: 0 วัน, 0.00, total 50,500.00', async () => {
    const { fixture } = await renderWith(C2);

    expect(text(fixture, 'ui-miniloan-005-accrued-interest')).toBe('0 วัน — 0.00');
    expect(text(fixture, 'ui-miniloan-005-total-payoff-amount')).toBe('50,500.00');
  });

  /** AC-miniloan-075 · GD-miniloan-005 C3 — one day past the boundary, and the total moves. */
  it('shows GD-miniloan-005 C3 exactly: 1 วัน, 34.25, total 50,534.25', async () => {
    const { fixture } = await renderWith(C3);

    expect(text(fixture, 'ui-miniloan-005-accrued-interest')).toBe('1 วัน — 34.25');
    expect(text(fixture, 'ui-miniloan-005-total-payoff-amount')).toBe('50,534.25');
  });

  /**
   * The date on screen is the server's echo, not the one the page asked with — the same reasoning
   * that put submittedAt on the application DTO in FE-miniloan-021. Here the request carries today
   * and the response says 2026-06-25; the screen must show the response.
   */
  it('displays the closing date the API returned, not the one it sent', async () => {
    const { fixture, request } = await renderWith(C1);

    const asked = request.request.params.get('closingDate');
    expect(asked).not.toBeNull();
    expect(text(fixture, 'ui-miniloan-005-quote-calculated-at')).toBe('2026-06-25');
    expect(text(fixture, 'ui-miniloan-005-quote-calculated-at')).not.toBe(asked);
  });

  /**
   * AC-miniloan-071 — "เป็นการแสดงยอดอย่างเดียว ไม่เปลี่ยนสถานะบัญชีและไม่สร้างรายการชำระใดๆ".
   * One GET goes out and nothing else; httpTesting.verify() in afterEach would fail on any extra.
   */
  it('only reads: one GET for the quote and no other call', async () => {
    const { request } = await renderWith(C1);

    expect(request.request.method).toBe('GET');
  });

  /**
   * AC-miniloan-072's provable half — the page offers no way to close the account. screens.json
   * declares one action here (back-to-schedule) and that is the only one rendered; the sentence the
   * criterion quotes has no field in screens.json to hold it, which is a card for design.
   */
  it('offers no close-account action, only the declared back link', async () => {
    const { fixture } = await renderWith(C1);
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelectorAll('button')).toHaveLength(0);
    const actions = [...root.querySelectorAll('a[data-testid]')].map((a) =>
      a.getAttribute('data-testid'),
    );
    expect(actions).toEqual(['ui-miniloan-005-back-to-schedule']);
  });

  /**
   * screens.json unauthorized. The sentence is this screen's own — UI-miniloan-004 answers
   * "ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้" for the same rule, and the two must not drift into one.
   */
  it('shows this screen"s own refusal sentence when the account is not the caller"s', async () => {
    const fixture = TestBed.createComponent(EarlyClosureQuoteComponent);
    fixture.componentRef.setInput('id', 'someone-elses-account');
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting
      .expectOne(
        (candidate) =>
          candidate.url === `${API_BASE_URL}/loan-accounts/someone-elses-account/payoff-quote`,
      )
      .flush(
        {
          code: 'PAYOFF_QUOTE_APPLICANT_ONLY',
          message: 'ไม่มีสิทธิ์ขอยอดปิดบัญชีก่อนกำหนด — ทำได้เฉพาะเจ้าของบัญชี',
        },
        { status: 403, statusText: 'Forbidden' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.notAllowed()).toBe(
      'ไม่มีสิทธิ์ขอยอดปิดบัญชีก่อนกำหนด — ทำได้เฉพาะเจ้าของบัญชี',
    );
    expect(fixture.componentInstance.quote()).toBeNull();
  });

  /** screens.json error — a failed calculation offers a retry and is not the unauthorized page. */
  it('separates a refusal from a failed calculation', async () => {
    const fixture = TestBed.createComponent(EarlyClosureQuoteComponent);
    fixture.componentRef.setInput('id', 'acc-1');
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting
      .expectOne((candidate) => candidate.url === `${API_BASE_URL}/loan-accounts/acc-1/payoff-quote`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect(fixture.componentInstance.notAllowed()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('คำนวณยอดไม่สำเร็จ');
  });
});

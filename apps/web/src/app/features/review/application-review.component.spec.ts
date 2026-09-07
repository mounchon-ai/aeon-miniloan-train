import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApplicationReviewComponent } from './application-review.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { ApplicationDetail } from '../../core/services/loan-application.service';

/**
 * UI-miniloan-007 (UC-miniloan-005 · 006 · 007 · 009).
 *
 * <p><b>What is deliberately NOT measured here.</b> AC-miniloan-001/002/003, 093–097, 100–106 and
 * 129/130 are schedule, EMI, rate-version and rounding criteria, and they are proved where the
 * arithmetic is — {@code AmortizationScheduleServiceTest}, {@code DisbursementServiceTest} and
 * {@code CreditAssessmentServiceTest}, against GD-miniloan-001..003. Re-asserting a golden figure
 * from a Spring-free browser test would be a second answer to a question already signed off, which
 * is the thing gate 87 exists to stop. What this file measures is the web half: that the officer's
 * press produces exactly one request, that the sentence the API answers with reaches the screen
 * unchanged, and that a refusal changes nothing on the page but the message.
 *
 * <p><b>AC-miniloan-054 is measured as an absence.</b> The criterion has the officer lower the
 * approved amount to 150,000 and approve again, but screens.json declares two capture fields on this
 * screen and mock minted ids for exactly those two. An amount input here would be a control nobody
 * asked for (DV17 Class B) carrying an id nobody minted (gate 37), so what is provable is that the
 * screen has no such field and that approve sends no amount — the API then approves at the amount
 * requested. It is a card for design.
 */
describe('ApplicationReviewComponent (UI-miniloan-007)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationReviewComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const DETAIL: ApplicationDetail = {
    application: {
      id: 'app-1',
      status: 'UnderReview',
      band: null,
      fullName: 'สมชาย ใจดี',
      age: 35,
      monthlyIncome: '30000.00',
      currentEmploymentMonths: 24,
      existingMonthlyDebt: '1000.00',
      requestedAmount: '100000.00',
      requestedTermMonths: 12,
      approvedAmount: null,
      approvedBy: null,
      approvedAt: null,
      rejectionReason: null,
      rejectedBy: null,
      rejectedAt: null,
      cancellationReason: null,
      cancelledBy: null,
      cancelledAt: null,
      submittedAt: '2026-06-01T00:00:00Z',
      createdAt: '2026-06-01T00:00:00Z',
      updatedAt: '2026-06-01T00:00:00Z',
    },
    assessment: {
      band: 'B',
      maxApprovableAmount: '150000.00',
      dtiRatio: '0.0333',
      dtiShown: '3.33%',
      reasons: ['อายุอยู่ในเกณฑ์', 'รายได้ผ่านเกณฑ์'],
      assessedAt: '2026-06-01T00:00:00Z',
    },
    assessmentNote: null,
    assignment: { loanOfficerId: 'ROLE-002', assignedBy: 'ROLE-003', assignedAt: '2026-06-01T00:00:00Z' },
  };

  function render(detail: ApplicationDetail = DETAIL, id = 'app-1') {
    const fixture = TestBed.createComponent(ApplicationReviewComponent);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      httpTesting.expectOne(`${API_BASE_URL}/applications/${id}`).flush(detail);
      fixture.detectChanges();
      return fixture;
    });
  }

  const el = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  const text = (fixture: { nativeElement: unknown }, id: string) =>
    el(fixture, id)?.textContent?.trim();

  const press = (fixture: { nativeElement: unknown }, id: string) =>
    (el(fixture, id) as HTMLButtonElement).click();

  it('renders every control mock minted for this screen', async () => {
    const fixture = await render();

    for (const id of [
      'ui-miniloan-007-ent-001-full-name',
      'ui-miniloan-007-ent-002-age',
      'ui-miniloan-007-ent-002-monthly-income',
      'ui-miniloan-007-ent-002-current-employment-months',
      'ui-miniloan-007-ent-002-existing-monthly-debt',
      'ui-miniloan-007-ent-002-requested-amount',
      'ui-miniloan-007-ent-002-requested-term-months',
      'ui-miniloan-007-ent-003-band',
      'ui-miniloan-007-ent-003-max-approvable-amount',
      'ui-miniloan-007-ent-003-dti-ratio',
      'ui-miniloan-007-ent-003-reasons',
      'ui-miniloan-007-ent-002-rejection-reason',
      'ui-miniloan-007-ent-002-cancellation-reason',
      'ui-miniloan-007-approve',
      'ui-miniloan-007-reject',
      'ui-miniloan-007-cancel',
      'ui-miniloan-007-disburse',
    ]) {
      expect(el(fixture, id)).not.toBeNull();
    }
  });

  /**
   * The assessment zone shows what the API decided. The DTI rendered is the API's own display
   * string, not the raw ratio formatted here — BR-miniloan-002@v1 is decided on the API side and how
   * it reads is part of that answer.
   */
  it('shows the assessment the API sent, including its own DTI wording', async () => {
    const fixture = await render();

    expect(text(fixture, 'ui-miniloan-007-ent-003-band')).toBe('B');
    expect(text(fixture, 'ui-miniloan-007-ent-003-max-approvable-amount')).toBe('150000.00');
    expect(text(fixture, 'ui-miniloan-007-ent-003-dti-ratio')).toBe('3.33%');
    expect(text(fixture, 'ui-miniloan-007-ent-003-reasons')).toContain('อายุอยู่ในเกณฑ์');
  });

  /** AC-miniloan-049 — "อนุมัติโดย ก. เมื่อ {วันที่เวลา}", with both taken from the API's answer. */
  it('approves with one call and shows who approved and when', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-007-approve');
    const request = httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/approve`);
    request.flush({
      id: 'app-1',
      status: 'Approved',
      approvedAmount: '100000.00',
      approvedBy: 'ROLE-002',
      approvedAt: '2026-06-02T09:00:00Z',
    });
    fixture.detectChanges();

    expect(request.request.method).toBe('POST');
    expect(fixture.componentInstance.status()).toBe('Approved');
    expect(fixture.componentInstance.outcome()).toBe('อนุมัติโดย ROLE-002 เมื่อ 2026-06-02T09:00:00Z');
  });

  /**
   * AC-miniloan-054's provable half. There is no amount field on this screen, and approve sends no
   * amount — API-007 reads a body without one as "at the amount that was requested". A screen that
   * invented an input would pass a test written to its own invention.
   */
  it('sends no approved amount, because the screen declares no field for one', async () => {
    const fixture = await render();
    const root = fixture.nativeElement as HTMLElement;

    const captures = [...root.querySelectorAll('input, textarea')].map((field) =>
      field.getAttribute('data-testid'),
    );
    expect(captures).toEqual([
      'ui-miniloan-007-ent-002-rejection-reason',
      'ui-miniloan-007-ent-002-cancellation-reason',
    ]);

    press(fixture, 'ui-miniloan-007-approve');
    const request = httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/approve`);
    expect(request.request.body).toEqual({});
    request.flush({
      id: 'app-1',
      status: 'Approved',
      approvedAmount: '100000.00',
      approvedBy: 'ROLE-002',
      approvedAt: '2026-06-02T09:00:00Z',
    });
  });

  /**
   * AC-miniloan-053 — "อนุมัติไม่ได้ — จำนวนเงินที่ขอ 150,001 บาท เกินวงเงินอนุมัติสูงสุด 150,000
   * บาท กรุณาปรับวงเงินก่อน". The sentence is the API's and reaches the page unchanged, and the
   * status does not move: the criterion says the application stays UnderReview.
   */
  it('shows the API refusal for an amount over the ceiling and leaves the status alone', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-007-approve');
    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/approve`).flush(
      {
        code: 'AMOUNT_EXCEEDS_MAX_APPROVABLE',
        message:
          'อนุมัติไม่ได้ — จำนวนเงินที่ขอ 150,001 บาท เกินวงเงินอนุมัติสูงสุด 150,000 บาท กรุณาปรับวงเงินก่อน',
      },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.commandError()).toBe(
      'อนุมัติไม่ได้ — จำนวนเงินที่ขอ 150,001 บาท เกินวงเงินอนุมัติสูงสุด 150,000 บาท กรุณาปรับวงเงินก่อน',
    );
    expect(fixture.componentInstance.status()).toBe('UnderReview');
    expect(fixture.componentInstance.outcome()).toBeNull();
  });

  /** AC-miniloan-055 — the reason typed on the page travels, and comes back in the sentence. */
  it('rejects with the typed reason and shows it back', async () => {
    const fixture = await render();

    fixture.componentInstance.rejectionReason.setValue('ภาระหนี้ต่อรายได้สูงเกินเกณฑ์');
    press(fixture, 'ui-miniloan-007-reject');

    const request = httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/reject`);
    expect(request.request.body).toEqual({ reason: 'ภาระหนี้ต่อรายได้สูงเกินเกณฑ์' });
    request.flush({
      id: 'app-1',
      status: 'Rejected',
      rejectionReason: 'ภาระหนี้ต่อรายได้สูงเกินเกณฑ์',
      rejectedBy: 'ROLE-002',
      rejectedAt: '2026-06-02T09:00:00Z',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe(
      'ปฏิเสธโดย ROLE-002 เมื่อ 2026-06-02T09:00:00Z · เหตุผล: ภาระหนี้ต่อรายได้สูงเกินเกณฑ์',
    );
  });

  /**
   * AC-miniloan-056 · AC-miniloan-062 — a blank reason is SENT, not stopped here. "การปฏิเสธเกิดที่
   * ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ": a browser-side guard would make the criterion unmeasurable
   * and would let a caller bypassing the page through.
   */
  it('sends an empty reason rather than blocking it, and shows the API refusal', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-007-reject');
    const request = httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/reject`);
    expect(request.request.body).toEqual({ reason: '' });
    request.flush(
      { code: 'REJECTION_REASON_REQUIRED', message: 'ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ' },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.commandError()).toBe('ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ');
    expect(fixture.componentInstance.status()).toBe('UnderReview');
  });

  /**
   * AC-miniloan-047 · AC-miniloan-067 — the sentence, and the three buttons gone. The cancel action's
   * destinationRef is UI-miniloan-006, but the criterion puts the message on THIS screen, so the page
   * stays; asserting the buttons are absent is what separates "gone" from "disabled".
   */
  it('cancels, shows the closing sentence, and drops approve, reject and disburse', async () => {
    const fixture = await render();

    fixture.componentInstance.cancellationReason.setValue('ผู้สมัครแจ้งขอถอนเรื่อง');
    press(fixture, 'ui-miniloan-007-cancel');
    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/cancel`).flush({
      id: 'app-1',
      status: 'Cancelled',
      cancellationReason: 'ผู้สมัครแจ้งขอถอนเรื่อง',
      cancelledBy: 'ROLE-002',
      cancelledAt: '2026-06-02T09:00:00Z',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toContain(
      'ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง',
    );
    expect(fixture.componentInstance.outcome()).toContain('เหตุผล: ผู้สมัครแจ้งขอถอนเรื่อง');
    expect(el(fixture, 'ui-miniloan-007-approve')).toBeNull();
    expect(el(fixture, 'ui-miniloan-007-reject')).toBeNull();
    expect(el(fixture, 'ui-miniloan-007-disburse')).toBeNull();
  });

  /** AC-miniloan-058 — "เบิกจ่ายเรียบร้อย — เปิดบัญชีสินเชื่อเลขที่ {เลขบัญชี}", with the API's number. */
  it('disburses and names the account the API opened', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-007-disburse');
    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/disburse`).flush({
      accountNumber: 'acc-77',
      applicationStatus: 'Disbursed',
      accountStatus: 'Active',
      principalAmount: '100000.00',
      termMonths: 12,
      disbursedAt: '2026-06-02T09:00:00Z',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.status()).toBe('Disbursed');
    expect(fixture.componentInstance.outcome()).toBe(
      'เบิกจ่ายเรียบร้อย — เปิดบัญชีสินเชื่อเลขที่ acc-77',
    );
  });

  /**
   * AC-miniloan-131 · AC-miniloan-132 · BR-miniloan-042@v1. A timeout has no body to quote, so the
   * criterion's own sentence is shown, the spinner stops, and — the part that matters — NOTHING goes
   * out afterwards. afterEach's verify() is what proves the absence of a background retry: a queued
   * second attempt would appear there as an unexpected request.
   */
  it('fails immediately on a timeout and never retries by itself', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-007-disburse');
    httpTesting
      .expectOne(`${API_BASE_URL}/applications/app-1/disburse`)
      .error(new ProgressEvent('timeout'), { status: 0, statusText: 'Unknown Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.commandError()).toBe(
      'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง',
    );
    expect(fixture.componentInstance.busy()).toBeNull();
    expect(fixture.componentInstance.status()).toBe('UnderReview');
  });

  /**
   * AC-miniloan-132's other half — the officer presses again themselves and it succeeds ONCE. The
   * second press is a second request because a person made it, which is the distinction the criterion
   * draws: one manual retry, never a queue.
   */
  it('lets the officer press again after a failure, and that is one more call, not two', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-007-disburse');
    httpTesting
      .expectOne(`${API_BASE_URL}/applications/app-1/disburse`)
      .error(new ProgressEvent('timeout'), { status: 0, statusText: 'Unknown Error' });
    fixture.detectChanges();

    press(fixture, 'ui-miniloan-007-disburse');
    const second = httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/disburse`);
    second.flush({
      accountNumber: 'acc-77',
      applicationStatus: 'Disbursed',
      accountStatus: 'Active',
      principalAmount: '100000.00',
      termMonths: 12,
      disbursedAt: '2026-06-02T09:00:00Z',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.status()).toBe('Disbursed');
    expect(fixture.componentInstance.commandError()).toBeNull();
  });

  /**
   * screens.json loading — "ปุ่มที่กดถูก disable … ป้องกันกดซ้ำ" (BR-miniloan-043@v1). Only the
   * pressed button is disabled: disabling all four would hide which command is running.
   */
  it('disables only the button that was pressed while it is in flight', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-007-disburse');
    fixture.detectChanges();

    expect((el(fixture, 'ui-miniloan-007-disburse') as HTMLButtonElement).disabled).toBe(true);
    expect((el(fixture, 'ui-miniloan-007-approve') as HTMLButtonElement).disabled).toBe(false);

    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/disburse`).flush({
      accountNumber: 'acc-77',
      applicationStatus: 'Disbursed',
      accountStatus: 'Active',
      principalAmount: '100000.00',
      termMonths: 12,
      disbursedAt: '2026-06-02T09:00:00Z',
    });
    fixture.detectChanges();
  });

  /**
   * screens.json unauthorized — "ไม่มีสิทธิ์ดำเนินการกับใบสมัครนี้ เมื่อพยายามเปิดใบที่มอบหมายให้
   * Loan Officer คนอื่น". The API's sentence is shown and not one field of the application appears;
   * FE-miniloan-023 added that refusal to {@code findDetail} because ACL-028 declares it.
   */
  it('shows the API refusal and no application data for another officer"s application', async () => {
    const fixture = TestBed.createComponent(ApplicationReviewComponent);
    fixture.componentRef.setInput('id', 'someone-elses');
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(`${API_BASE_URL}/applications/someone-elses`).flush(
      { code: 'APPLICATION_NOT_VISIBLE', message: 'ไม่มีสิทธิ์เข้าถึงใบสมัครนี้' },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.notAllowed()).toBe('ไม่มีสิทธิ์เข้าถึงใบสมัครนี้');
    expect(fixture.componentInstance.detail()).toBeNull();
    expect(el(fixture, 'ui-miniloan-007-ent-001-full-name')).toBeNull();
    expect(el(fixture, 'ui-miniloan-007-approve')).toBeNull();
  });

  /** screens.json error and unauthorized are different branches; only error offers a retry. */
  it('separates a refusal from a failed load', async () => {
    const fixture = TestBed.createComponent(ApplicationReviewComponent);
    fixture.componentRef.setInput('id', 'app-1');
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting
      .expectOne(`${API_BASE_URL}/applications/app-1`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect(fixture.componentInstance.notAllowed()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดใบสมัครไม่สำเร็จ');
  });
});

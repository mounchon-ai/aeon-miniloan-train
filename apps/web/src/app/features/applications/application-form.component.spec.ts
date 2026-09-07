import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ApplicationFormComponent } from './application-form.component';
import { API_BASE_URL } from '../../core/api-base-url';

/**
 * UI-miniloan-001 (UC-miniloan-001 · UC-miniloan-002 · UC-miniloan-028).
 *
 * <p><b>What a web test can and cannot prove.</b> AC-miniloan-023/024/025/026/029/030/033 are
 * decided by apps/api — the ranges, the missing-field list, the state change — and
 * {@code LoanApplicationSubmitServiceTest} and {@code CreditAssessmentServiceTest} already measure
 * them there. What only this file can prove is the half those cannot see: that the browser ASKS
 * rather than decides (BR-miniloan-027@v1 · REQ-miniloan-006), that the API's refusal reaches the
 * page word for word instead of being replaced by one written here, and that the controls carry the
 * testids mock minted.
 *
 * <p>So the assertions below are about traffic and rendering. A boundary value is sent to the API
 * and the request is inspected; a refusal is flushed back and the page is read.
 */
describe('ApplicationFormComponent (UI-miniloan-001)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        // A successful submit navigates to UI-miniloan-003 (screens.json destinationRef), so the
        // test router needs that path to exist or the navigation rejects. The route renders nothing
        // here — what is under test is that the page goes there, not what that page shows.
        provideRouter([{ path: 'applications/:id', children: [] }]),
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  function render() {
    const fixture = TestBed.createComponent(ApplicationFormComponent);
    fixture.detectChanges();
    return fixture;
  }

  const testid = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  // ── the controls mock minted ─────────────────────────────────────────────

  /**
   * qa selects by these strings and they can never change (gate 37), so every one design declared
   * for this screen has to be in the DOM — not merely known to the component.
   */
  it('renders every control mock minted for this screen', () => {
    const fixture = render();

    for (const id of [
      'ui-miniloan-001-ent-001-full-name',
      'ui-miniloan-001-ent-002-age',
      'ui-miniloan-001-ent-002-monthly-income',
      'ui-miniloan-001-ent-002-current-employment-months',
      'ui-miniloan-001-ent-002-existing-monthly-debt',
      'ui-miniloan-001-ent-002-requested-amount',
      'ui-miniloan-001-ent-002-requested-term-months',
      'ui-miniloan-001-estimated-max-approved-amount',
      'ui-miniloan-001-save-draft',
      'ui-miniloan-001-submit',
    ]) {
      expect(testid(fixture, id)).not.toBeNull();
    }
  });

  // ── AC-miniloan-117 · the browser asks, it does not multiply ─────────────

  /**
   * "หน้าจอเรียก API เพื่อขอตัวเลขทุกครั้ง ไม่ได้คูณ 5 เองในเบราว์เซอร์" — the request is what is
   * asserted, and the rendered figure is the one the API sent back, not 30000 x 5 computed here.
   */
  it('asks API-021 for the ceiling whenever the income changes and renders what came back', () => {
    const fixture = render();

    fixture.componentInstance.onIncomeChange('30000.00');

    const request = httpTesting.expectOne(`${API_BASE_URL}/credit-assessments/preview`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ monthlyIncome: '30000.00' });
    request.flush({ maxApprovableAmount: '150000.00' });
    fixture.detectChanges();

    expect(testid(fixture, 'ui-miniloan-001-estimated-max-approved-amount')?.textContent).toContain(
      '150000.00',
    );
  });

  /** A second change is a second call — the criterion says "ทุกครั้งที่ตัวเลขเปลี่ยน". */
  it('asks again on every subsequent change rather than reusing the first answer', () => {
    const fixture = render();

    fixture.componentInstance.onIncomeChange('30000.00');
    httpTesting.expectOne(`${API_BASE_URL}/credit-assessments/preview`).flush({
      maxApprovableAmount: '150000.00',
    });

    fixture.componentInstance.onIncomeChange('250000.00');
    const second = httpTesting.expectOne(`${API_BASE_URL}/credit-assessments/preview`);
    expect(second.request.body).toEqual({ monthlyIncome: '250000.00' });
    second.flush({ maxApprovableAmount: '1000000.00' });
  });

  // ── AC-miniloan-118 · a failed call shows a sentence, never a guess ──────

  /**
   * "ไม่แสดงตัวเลขที่คำนวณเอง แต่แสดงข้อผิดพลาด — ตอนนี้ดึงข้อมูลวงเงินไม่ได้ กรุณาลองใหม่อีกครั้ง".
   * The figure must be absent, not merely stale: a page that kept showing 150,000 after the call
   * failed would be showing a number the API never confirmed.
   */
  it('shows the error sentence and no figure when the preview call fails', () => {
    const fixture = render();

    fixture.componentInstance.onIncomeChange('30000.00');
    httpTesting
      .expectOne(`${API_BASE_URL}/credit-assessments/preview`)
      .flush({}, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    const rendered = testid(fixture, 'ui-miniloan-001-estimated-max-approved-amount')?.textContent;
    expect(rendered).toContain('ตอนนี้ดึงข้อมูลวงเงินไม่ได้ กรุณาลองใหม่อีกครั้ง');
    expect(rendered).not.toContain('150000');
    expect(fixture.componentInstance.maxApprovablePreview()).toBeNull();
  });

  // ── AC-miniloan-032 · 033 · a draft is saved as typed ────────────────────

  /**
   * "กรอกเฉพาะชื่อ … บันทึกสำเร็จ … ไม่มีข้อความเตือนเรื่องช่องที่ยังไม่ได้กรอก" — the page sends what
   * was typed and asserts no local validator stopped it first. Whether the API accepts it is
   * LoanApplicationDraftServiceTest's question.
   */
  it('sends a draft with only the name filled in, and checks nothing itself', () => {
    const fixture = render();
    fixture.componentInstance.form.patchValue({ fullName: 'สมชาย ใจดี' });

    fixture.componentInstance.onSaveDraft();

    const request = httpTesting.expectOne(`${API_BASE_URL}/applications`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.fullName).toBe('สมชาย ใจดี');
    expect(request.request.body.requestedAmount).toBeNull();
    request.flush({ id: 'app-1', status: 'Draft' });
    fixture.detectChanges();

    expect(fixture.componentInstance.message()).toBe('บันทึกร่างเรียบร้อย');
    expect(fixture.componentInstance.fieldErrors()).toEqual([]);
  });

  /**
   * AC-miniloan-033 — 5,000 บาท is below BR-miniloan-004@v1's range and is still sent unchanged.
   * A client-side range check here would refuse a draft the rule explicitly allows.
   */
  it('sends an out-of-range amount on save-draft instead of refusing it locally', () => {
    const fixture = render();
    fixture.componentInstance.form.patchValue({ requestedAmount: '5000' });

    fixture.componentInstance.onSaveDraft();

    const request = httpTesting.expectOne(`${API_BASE_URL}/applications`);
    expect(request.request.body.requestedAmount).toBe('5000');
    request.flush({ id: 'app-1', status: 'Draft' });
  });

  // ── AC-miniloan-026 · 030 · the API's words, all of them ─────────────────

  /**
   * "แสดงข้อความระบุช่องที่ขาด ครบทุกช่อง ไม่ใช่ช่องแรกช่องเดียว" — the API sends `missingFields`
   * precisely so every empty box is named at once, and this page renders all of them.
   */
  it('renders every missing field the API named, not just the first', () => {
    const fixture = render();
    fixture.componentInstance.applicationId.set('app-1');

    fixture.componentInstance.onSubmit();
    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/submit`).flush(
      {
        code: 'APPLICATION_INCOMPLETE',
        message: 'ยื่นใบสมัครไม่ได้ — ยังกรอกไม่ครบ: อายุงาน, จำนวนงวด',
        missingFields: ['อายุงาน', 'จำนวนงวด'],
      },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.fieldErrors()).toEqual(['อายุงาน', 'จำนวนงวด']);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('อายุงาน');
    expect(text).toContain('จำนวนงวด');
  });

  // ── AC-miniloan-029 · 031 · submitted is locked ──────────────────────────

  /**
   * "ทุกช่องกรอกกลายเป็นอ่านอย่างเดียว" — after the API reports Submitted the inputs are disabled.
   * The API refuses an edit as well (AC-miniloan-031's other half, measured in apps/api); this is
   * the screen half of the same sentence.
   */
  it('locks every input once the API reports the application as Submitted', () => {
    const fixture = render();
    fixture.componentInstance.applicationId.set('app-1');

    fixture.componentInstance.onSubmit();
    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/submit`).flush({
      application: { id: 'app-1', status: 'Submitted' },
      assessment: null,
      assessmentNote: null,
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.locked()).toBe(true);
    expect(fixture.componentInstance.form.disabled).toBe(true);
    const input = testid(fixture, 'ui-miniloan-001-ent-002-requested-amount') as HTMLInputElement;
    expect(input.disabled).toBe(true);
  });

  // ── AC-miniloan-133 · the repeat is shown, never swallowed ───────────────

  /**
   * "ถูกปฏิเสธพร้อมข้อความที่ผู้ใช้เห็น · ไม่ใช่คืนผลของครั้งแรกเงียบๆ" — the criterion proves
   * behaviour, not wording (DQ-miniloan-010 is still open), so what is asserted is that whatever the
   * API refused with reaches the page.
   */
  it('shows the API refusal when the same submit is fired twice', () => {
    const fixture = render();
    fixture.componentInstance.applicationId.set('app-1');

    fixture.componentInstance.onSubmit();
    httpTesting
      .expectOne(`${API_BASE_URL}/applications/app-1/submit`)
      .flush(
        { code: 'DUPLICATE_COMMAND', message: 'คำสั่งนี้ถูกดำเนินการไปแล้ว' },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.message()).toBe('คำสั่งนี้ถูกดำเนินการไปแล้ว');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'คำสั่งนี้ถูกดำเนินการไปแล้ว',
    );
  });
});

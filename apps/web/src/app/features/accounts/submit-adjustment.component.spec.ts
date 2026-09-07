import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SubmitAdjustmentComponent } from './submit-adjustment.component';
import { API_BASE_URL } from '../../core/api-base-url';

/**
 * UI-miniloan-012's adjustment-form zone (AC-miniloan-076 · 077 · 078 · 080 · 081).
 *
 * <p><b>What only a web test can prove.</b> Whether the requester and the approver must be different
 * people (BR-miniloan-049@v1), and whether an unset approver role refuses the whole feature
 * (BR-miniloan-040@v1), are the API's questions and ClosedAccountAdjustmentServiceTest answers both.
 * What is measured here is that this page files a request and decides nothing: every sentence it
 * shows afterwards is the API's, the five field names are ENT-010's declared enum rather than words
 * invented here, and the form is sent even when it is empty — because AC-miniloan-080 and
 * AC-miniloan-081 both require the refusal to happen at the API rather than by declining to call.
 */
describe('SubmitAdjustmentComponent (UI-miniloan-012 · adjustment-form)', () => {
  let httpTesting: HttpTestingController;

  const ACCOUNT = 'acc-1';
  const URL = `${API_BASE_URL}/loan-accounts/${ACCOUNT}/adjustments`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubmitAdjustmentComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  function render() {
    const fixture = TestBed.createComponent(SubmitAdjustmentComponent);
    fixture.componentRef.setInput('accountId', ACCOUNT);
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  it('renders every control mock minted for this zone', () => {
    const fixture = render();

    for (const id of [
      'ui-miniloan-012-ent-010-target-record-id',
      'ui-miniloan-012-ent-010-field-name',
      'ui-miniloan-012-ent-010-old-value',
      'ui-miniloan-012-ent-010-new-value',
      'ui-miniloan-012-submit-adjustment',
    ]) {
      expect(el(fixture, id)).not.toBeNull();
    }
  });

  /**
   * ENT-010's enum, as datamodel.json declares it and as apps/api deserialises it. The DECLARED
   * names ("Payment.amount"), not the Java constant names, and nothing here translates them: no
   * document in design gives them a Thai label, and inventing one would put a word in the client's
   * mouth.
   */
  it('offers exactly the five declared field names', () => {
    const fixture = render();
    const select = el(fixture, 'ui-miniloan-012-ent-010-field-name') as HTMLSelectElement;

    expect([...select.options].map((option) => option.value)).toEqual([
      '',
      'LoanAccount.closedAt',
      'LoanAccount.closeReason',
      'LoanAccount.assignedOperationsId',
      'Payment.amount',
      'Payment.recordedAt',
    ]);
  });

  /** AC-miniloan-076's first step: the four capture fields travel exactly as they were filled in. */
  it('sends the four capture fields ENT-010 requires', () => {
    const fixture = render();
    fixture.componentInstance.form.setValue({
      targetRecordId: 'pay-12',
      fieldName: 'Payment.amount',
      oldValue: '9583.33',
      newValue: '9583.30',
    });
    (el(fixture, 'ui-miniloan-012-submit-adjustment') as HTMLButtonElement).click();

    const request = httpTesting.expectOne(URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      targetRecordId: 'pay-12',
      fieldName: 'Payment.amount',
      oldValue: '9583.33',
      newValue: '9583.30',
    });

    request.flush(
      {
        id: 'adj-1',
        loanAccountId: ACCOUNT,
        targetRecordId: 'pay-12',
        fieldName: 'Payment.amount',
        oldValue: '9583.33',
        newValue: '9583.30',
        requestedBy: 'ROLE-004',
        requestedAt: '2026-06-01T00:00:00Z',
        status: 'Pending',
        approverRole: 'LoanOfficer',
        message: 'ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก LoanOfficer',
      },
      { status: 201, statusText: 'Created' },
    );
    fixture.detectChanges();

    // AC-miniloan-076's sentence is the API's, approver role and all — never composed here.
    expect(fixture.componentInstance.outcome()).toBe(
      'ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก LoanOfficer',
    );
  });

  /**
   * AC-miniloan-080 · AC-miniloan-081. The refusal comes back from the server and is rendered
   * untouched; the point of both criteria is that no request is created and no default approver is
   * chosen, and a page that declined to call would look identical to one that had been refused.
   */
  it('sends an empty form too, and renders the unset-approver refusal in the API words', () => {
    const fixture = render();
    (el(fixture, 'ui-miniloan-012-submit-adjustment') as HTMLButtonElement).click();

    const request = httpTesting.expectOne(URL);
    expect(request.request.body).toEqual({
      targetRecordId: '',
      fieldName: '',
      oldValue: '',
      newValue: '',
    });

    request.flush(
      {
        code: 'APPROVER_ROLE_NOT_SET',
        message:
          'ยังไม่ได้ตั้ง role ผู้อนุมัติ — ใช้ฟีเจอร์แก้ข้อมูลบัญชีที่ปิดแล้วไม่ได้ · ให้ Loan Officer ตั้งค่า role ผู้อนุมัติก่อน',
      },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(
      'ยังไม่ได้ตั้ง role ผู้อนุมัติ — ใช้ฟีเจอร์แก้ข้อมูลบัญชีที่ปิดแล้วไม่ได้ · ให้ Loan Officer ตั้งค่า role ผู้อนุมัติก่อน',
    );
    expect(fixture.componentInstance.outcome()).toBeNull();
  });

  /**
   * AC-miniloan-077 · AC-miniloan-078's screen halves. This zone offers one action, and it is the
   * request — there is no direct edit of the account and no delete. A control for either would need
   * a minted id and there is none (gate 37).
   */
  it('offers the request and nothing that edits or removes the account directly', () => {
    const fixture = render();
    const root = fixture.nativeElement as HTMLElement;

    expect([...root.querySelectorAll('button')].map((b) => b.getAttribute('data-testid'))).toEqual([
      'ui-miniloan-012-submit-adjustment',
    ]);
    expect(root.textContent).not.toContain('ลบบัญชี');
    expect(root.textContent).not.toContain('ยกเลิกบัญชี');
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdjustmentReviewComponent } from './adjustment-review.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { PendingAdjustment } from '../../core/services/closed-account-adjustment.service';
import { CurrentRoleService } from '../../core/services/current-role.service';

/**
 * UI-miniloan-014 (UC-miniloan-018 · AC-miniloan-076 · AC-miniloan-079 · AC-miniloan-084 ·
 * AC-miniloan-085).
 *
 * <p><b>What only a web test can prove.</b> That an approval writes the new value onto the account,
 * that a rejection leaves it alone, and that the API refuses a self-approval are
 * ClosedAccountAdjustmentDecisionServiceTest's and AdjustmentControllerTest's questions, answered
 * there against the database where BR-miniloan-038@v1 and BR-miniloan-049@v1 live. Re-deciding any
 * of it here would be a second answer to a settled question (gate 87).
 *
 * <p>What is measured here is the half those cannot see: AC-miniloan-079's SCREEN clause — the
 * approve button rendered and unpressable on a request the viewer filed, while reject stays pressable
 * because the rule is about approval — that the API's refusal still reaches the screen untouched on
 * the path that gets there, and that both outcome sentences are the API's rather than composed from
 * fields this page happens to hold.
 */
describe('AdjustmentReviewComponent (UI-miniloan-014)', () => {
  let httpTesting: HttpTestingController;
  let roles: CurrentRoleService;

  const ID = 'adj-1';
  const VIEW_URL = `${API_BASE_URL}/adjustments/${ID}`;
  const APPROVE_URL = `${API_BASE_URL}/adjustments/${ID}/approve`;
  const REJECT_URL = `${API_BASE_URL}/adjustments/${ID}/reject`;

  /** AC-miniloan-076's cast: ก. is Operations (ROLE-004), ข. is the approver (ROLE-005). */
  const REQUESTER = 'ROLE-004';
  const APPROVER = 'ROLE-005';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdjustmentReviewComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    roles = TestBed.inject(CurrentRoleService);
    roles.setRole(APPROVER);
  });

  afterEach(() => httpTesting.verify());

  const pending = (over: Partial<PendingAdjustment> = {}): PendingAdjustment => ({
    id: ID,
    loanAccountId: 'acc-1',
    targetRecordId: 'pay-12',
    fieldName: 'Payment.amount',
    oldValue: '9583.33',
    newValue: '9583.30',
    requestedBy: REQUESTER,
    requestedAt: '2026-06-01T00:00:00Z',
    status: 'Pending',
    approvedBy: null,
    approvedAt: null,
    ...over,
  });

  async function render(view: PendingAdjustment = pending()) {
    const fixture = TestBed.createComponent(AdjustmentReviewComponent);
    fixture.componentRef.setInput('id', ID);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(VIEW_URL).flush(view);
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  const button = (fixture: { nativeElement: unknown }, id: string) =>
    el(fixture, id) as HTMLButtonElement;

  const text = (fixture: { nativeElement: unknown }) =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('renders every control mock minted for this screen', async () => {
    const fixture = await render();

    for (const id of [
      'ui-miniloan-014-ent-010-field-name',
      'ui-miniloan-014-ent-010-old-value',
      'ui-miniloan-014-ent-010-new-value',
      'ui-miniloan-014-ent-010-requested-by',
      'ui-miniloan-014-ent-010-requested-at',
      'ui-miniloan-014-approve-adjustment',
      'ui-miniloan-014-reject-adjustment',
    ]) {
      expect(el(fixture, id)).not.toBeNull();
    }
  });

  /**
   * The old value and the new one side by side, which is the whole point of this screen — a template
   * that crossed them over would tell the approver to change 9583.30 into 9583.33.
   */
  it('shows both values in their own fields, and the requester with them', async () => {
    const fixture = await render();

    expect(el(fixture, 'ui-miniloan-014-ent-010-old-value')?.textContent?.trim()).toBe('9583.33');
    expect(el(fixture, 'ui-miniloan-014-ent-010-new-value')?.textContent?.trim()).toBe('9583.30');
    expect(el(fixture, 'ui-miniloan-014-ent-010-field-name')?.textContent?.trim()).toBe(
      'Payment.amount',
    );
    expect(el(fixture, 'ui-miniloan-014-ent-010-requested-by')?.textContent?.trim()).toBe(
      REQUESTER,
    );
  });

  /**
   * AC-miniloan-079's screen clause: "ปุ่ม 'อนุมัติ' บนคำขอที่ตัวเองยื่นแสดงเป็นสถานะกดไม่ได้". The
   * button is THERE and unpressable — not absent, which is what AC-miniloan-070 asked of a different
   * screen. Reject stays pressable on purpose: BR-miniloan-049@v1 is about approval, and disabling
   * this one too would be a rule invented here.
   */
  it('disables approve on a request the viewer filed, and leaves reject pressable', async () => {
    roles.setRole(REQUESTER);
    const fixture = await render(pending({ requestedBy: REQUESTER }));

    expect(el(fixture, 'ui-miniloan-014-approve-adjustment')).not.toBeNull();
    expect(button(fixture, 'ui-miniloan-014-approve-adjustment').disabled).toBe(true);
    expect(button(fixture, 'ui-miniloan-014-reject-adjustment').disabled).toBe(false);
    expect(fixture.componentInstance.ownRequest()).toBe(true);
    expect(text(fixture)).toContain('BR-miniloan-049@v1');
  });

  /** And somebody else's request is pressable, so the disable above is about WHO filed it. */
  it('leaves approve pressable on somebody else request', async () => {
    const fixture = await render();

    expect(button(fixture, 'ui-miniloan-014-approve-adjustment').disabled).toBe(false);
    expect(fixture.componentInstance.ownRequest()).toBe(false);
  });

  /**
   * AC-miniloan-079's API clause — "ทั้งจากหน้าจอ และด้วยการเรียก API … ทั้งสองทางถูกปฏิเสธ". The
   * disabled button is one half and this is the other: when the refusal does come back, the screen
   * renders the server's sentence word for word rather than one of its own, AND the request stays on
   * screen and stays Pending, which the criterion states explicitly.
   */
  it('renders the four-eyes refusal in the API words and keeps the request on screen', async () => {
    const fixture = await render();

    button(fixture, 'ui-miniloan-014-approve-adjustment').click();
    httpTesting.expectOne(APPROVE_URL).flush(
      {
        code: 'ADJUSTMENT_SELF_APPROVAL_REFUSED',
        message: 'อนุมัติคำขอของตัวเองไม่ได้ — ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้',
      },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(
      'อนุมัติคำขอของตัวเองไม่ได้ — ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้',
    );
    // still readable, still Pending — AC-miniloan-079's own words about what did NOT change
    expect(fixture.componentInstance.adjustment()?.status).toBe('Pending');
    expect(el(fixture, 'ui-miniloan-014-ent-010-old-value')?.textContent?.trim()).toBe('9583.33');
    expect(fixture.componentInstance.notAllowed()).toBeNull();
  });

  /**
   * AC-miniloan-076's second step. The sentence is the API's — it is built from the approval time
   * actually written to the row, which the service's note says in so many words — and this page
   * renders it. A page that assembled "การแก้ไขมีผลเมื่อ …" from its own approvedAt would be reading
   * the clock a second time.
   */
  it('sends the approval and shows AC-miniloan-076 sentence exactly as the API worded it', async () => {
    const fixture = await render();

    button(fixture, 'ui-miniloan-014-approve-adjustment').click();
    const request = httpTesting.expectOne(APPROVE_URL);
    expect(request.request.method).toBe('POST');

    request.flush({
      adjustment: pending({
        status: 'Approved',
        approvedBy: APPROVER,
        approvedAt: '2026-06-02T08:30:00Z',
      }),
      message: 'อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ 2026-06-02T08:30:00Z',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe(
      'อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ 2026-06-02T08:30:00Z',
    );
    expect(text(fixture)).toContain('อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ 2026-06-02T08:30:00Z');
  });

  /**
   * AC-miniloan-084's four-eyes record and AC-miniloan-085's premise, as far as these two screens can
   * see them. Pending arrives with both fields null; the decided row comes back carrying both, and
   * the requester and the approver are different people. Neither field is a declared control on this
   * screen and neither carries a testid — the per-account history page they belong on is
   * GAP-miniloan-010, open since 2026-09-06.
   */
  it('holds no approver while Pending and both after approval, by two different people', async () => {
    const fixture = await render();

    expect(fixture.componentInstance.adjustment()?.approvedBy).toBeNull();
    expect(fixture.componentInstance.adjustment()?.approvedAt).toBeNull();

    button(fixture, 'ui-miniloan-014-approve-adjustment').click();
    httpTesting.expectOne(APPROVE_URL).flush({
      adjustment: pending({
        status: 'Approved',
        approvedBy: APPROVER,
        approvedAt: '2026-06-02T08:30:00Z',
      }),
      message: 'อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ 2026-06-02T08:30:00Z',
    });
    fixture.detectChanges();

    const decided = fixture.componentInstance.adjustment();
    expect(decided?.approvedBy).toBe(APPROVER);
    expect(decided?.approvedAt).toBe('2026-06-02T08:30:00Z');
    expect(decided?.requestedBy).not.toBe(decided?.approvedBy);
  });

  /** UC-miniloan-018's alternate flow — its sentence is the API's too, and the old value stays. */
  it('sends the rejection and shows the API sentence for it', async () => {
    const fixture = await render();

    button(fixture, 'ui-miniloan-014-reject-adjustment').click();
    const request = httpTesting.expectOne(REJECT_URL);
    expect(request.request.method).toBe('POST');

    request.flush({
      adjustment: pending({ status: 'Rejected', approvedBy: APPROVER, approvedAt: '2026-06-02T08:30:00Z' }),
      message: 'ปฏิเสธคำขอปรับปรุงแล้ว — ค่าเดิมของบัญชียังคงอยู่',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe(
      'ปฏิเสธคำขอปรับปรุงแล้ว — ค่าเดิมของบัญชียังคงอยู่',
    );
    expect(el(fixture, 'ui-miniloan-014-ent-010-old-value')?.textContent?.trim()).toBe('9583.33');
  });

  /**
   * ACL-016's condition is STM-miniloan-004 state Pending, so a decided request offers neither
   * action. That is design's own condition rather than a rule added here, and the API refuses a
   * second decision anyway.
   */
  it('offers neither action once the request has been decided', async () => {
    const fixture = await render(
      pending({ status: 'Approved', approvedBy: APPROVER, approvedAt: '2026-06-02T08:30:00Z' }),
    );

    expect(button(fixture, 'ui-miniloan-014-approve-adjustment').disabled).toBe(true);
    expect(button(fixture, 'ui-miniloan-014-reject-adjustment').disabled).toBe(true);
    expect(fixture.componentInstance.decided()).toBe(true);
  });

  /** screens.json loading — the pressed button is disabled while its own call is in flight. */
  it('disables both actions while a decision is in flight', async () => {
    const fixture = await render();

    button(fixture, 'ui-miniloan-014-approve-adjustment').click();
    fixture.detectChanges();

    expect(button(fixture, 'ui-miniloan-014-approve-adjustment').disabled).toBe(true);
    expect(button(fixture, 'ui-miniloan-014-reject-adjustment').disabled).toBe(true);

    httpTesting.expectOne(APPROVE_URL).flush({
      adjustment: pending({ status: 'Approved', approvedBy: APPROVER, approvedAt: '2026-06-02T08:30:00Z' }),
      message: 'อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ 2026-06-02T08:30:00Z',
    });
    fixture.detectChanges();
  });

  /**
   * screens.json unauthorized, on the READ — ACL-016 · ACL-035. Nothing of the request is left on
   * screen, and the API's own sentence is what appears.
   */
  it('shows the API refusal and no request data when the read is refused', async () => {
    const fixture = TestBed.createComponent(AdjustmentReviewComponent);
    fixture.componentRef.setInput('id', ID);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(VIEW_URL).flush(
      {
        code: 'ADJUSTMENT_NOT_THE_APPROVER',
        message:
          'ไม่มีสิทธิ์พิจารณาคำขอปรับปรุงบัญชี — ทำได้เฉพาะผู้ถือ role ผู้อนุมัติที่ตั้งไว้ (LoanOfficer)',
      },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.notAllowed()).toBe(
      'ไม่มีสิทธิ์พิจารณาคำขอปรับปรุงบัญชี — ทำได้เฉพาะผู้ถือ role ผู้อนุมัติที่ตั้งไว้ (LoanOfficer)',
    );
    expect(el(fixture, 'ui-miniloan-014-ent-010-old-value')).toBeNull();
    expect(el(fixture, 'ui-miniloan-014-approve-adjustment')).toBeNull();
  });

  /**
   * This screen mints no control design did not declare. ผู้อนุมัติ and เวลาอนุมัติ are what
   * AC-miniloan-084 asks for and neither is a field here, so the roster is exactly mock's seven —
   * adding an eighth without a minted id would fail this.
   */
  it('mints no control design did not declare', async () => {
    const fixture = await render();
    const root = fixture.nativeElement as HTMLElement;

    expect(
      [...root.querySelectorAll('[data-testid]')].map((node) => node.getAttribute('data-testid')),
    ).toEqual([
      'ui-miniloan-014-ent-010-field-name',
      'ui-miniloan-014-ent-010-old-value',
      'ui-miniloan-014-ent-010-new-value',
      'ui-miniloan-014-ent-010-requested-by',
      'ui-miniloan-014-ent-010-requested-at',
      'ui-miniloan-014-approve-adjustment',
      'ui-miniloan-014-reject-adjustment',
    ]);
  });
});

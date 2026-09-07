import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UnassignedQueueComponent } from './unassigned-queue.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { LoanApplication } from '../../core/services/loan-application.service';

/**
 * UI-miniloan-010 (UC-miniloan-004 · UC-miniloan-008 · ACL-031).
 *
 * <p><b>What is deliberately NOT measured here.</b> AC-miniloan-064, 065, 066 and 092 are
 * BR-miniloan-032@v1 and BR-miniloan-031@v2 authority rules — who may approve once an application is
 * assigned, that equal rank is not enough, that an unassigned application is refused to everyone, and
 * that the right to cancel moves from the supervisor to the assigned officer the moment assignment
 * happens. Every one is proved where the rule lives: {@code ApplicationAssignmentServiceTest},
 * {@code LoanApplicationApprovalServiceTest} and {@code LoanApplicationCancellationServiceTest}, from
 * FE-miniloan-006 through FE-miniloan-009. The web half of all four is one behaviour — the API's
 * sentence reaches the page and the queue does not change itself — and it is asserted once below.
 *
 * <p><b>Two criteria are measurable only as far as the refusal, and the reason is design's.</b>
 * screens.json declares two actions on this screen and ZERO capture fields, so neither the Loan
 * Officer to assign to (AC-miniloan-064) nor the cancellation reason (AC-miniloan-090) can be
 * supplied from here. Inventing either control would be a control nobody asked for (DV17 Class B)
 * carrying a testid nobody minted (gate 37) — and the officer picker has no data source at all, since
 * no endpoint returns Loan Officers. What IS provable is that the page has no such field, that the
 * command is sent anyway, and that the API refuses with its own sentence. Two cards are open.
 */
describe('UnassignedQueueComponent (UI-miniloan-010)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UnassignedQueueComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const row = (over: Partial<LoanApplication>): LoanApplication => ({
    id: 'app-1',
    status: 'Submitted',
    band: null,
    assignedLoanOfficerId: null,
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
    ...over,
  });

  /** Two waiting to be handed out, one already held — ACL-031 returns all three. */
  const ALL: LoanApplication[] = [
    row({ id: 'app-1', fullName: 'สมชาย ใจดี', status: 'UnderReview', band: 'B' }),
    row({ id: 'app-2', fullName: 'สมหญิง รักเรียน', status: 'Submitted', band: null }),
    row({ id: 'app-3', fullName: 'สมปอง มีสุข', status: 'UnderReview', assignedLoanOfficerId: 'ROLE-002' }),
  ];

  function render(applications: LoanApplication[] = ALL) {
    const fixture = TestBed.createComponent(UnassignedQueueComponent);
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      httpTesting.expectOne(`${API_BASE_URL}/applications`).flush(applications);
      fixture.detectChanges();
      return fixture;
    });
  }

  const all = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelectorAll(`[data-testid="${id}"]`);

  const press = (fixture: { nativeElement: unknown }, id: string, index = 0) =>
    (all(fixture, id)[index] as HTMLButtonElement).click();

  it('renders every control mock minted for this screen', async () => {
    const fixture = await render();

    for (const id of [
      'ui-miniloan-010-ent-001-full-name',
      'ui-miniloan-010-ent-002-status',
      'ui-miniloan-010-ent-003-band',
      'ui-miniloan-010-ent-002-requested-amount',
      'ui-miniloan-010-assign-officer',
      'ui-miniloan-010-cancel-application',
    ]) {
      expect(all(fixture, id).length).toBeGreaterThan(0);
    }
  });

  /**
   * The screen's subject. ACL-031 is scope: all, so API-005 sends every application and the page
   * shows the ones nobody holds — the held one is dropped from the QUEUE, not from the caller's
   * scope. Asserted by name: a page that showed all three would still look like a queue.
   */
  it('shows only the applications nobody is assigned to', async () => {
    const fixture = await render();

    const names = [...all(fixture, 'ui-miniloan-010-ent-001-full-name')].map((cell) =>
      cell.textContent?.trim(),
    );
    expect(names).toEqual(['สมชาย ใจดี', 'สมหญิง รักเรียน']);
  });

  /** ENT-003 is optional on this screen — "แสดงเฉพาะใบที่ประเมินแล้ว" — and the row stays either way. */
  it('keeps a row whose application has not been assessed', async () => {
    const fixture = await render();

    const bands = [...all(fixture, 'ui-miniloan-010-ent-003-band')].map((cell) =>
      cell.textContent?.trim(),
    );
    expect(bands).toEqual(['B', '—']);
  });

  /**
   * AC-miniloan-064's provable half. There is no field on this screen that captures a Loan Officer,
   * and no endpoint that would populate one — so the command goes out with nothing and
   * BR-miniloan-032@v1 answers from the API. The absence is asserted first, because a later unit that
   * invented the picker would silently make this test measure its own invention.
   */
  it('has no field to choose an officer, and lets the API refuse the empty assignment', async () => {
    const fixture = await render();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelectorAll('input, textarea, select')).toHaveLength(0);

    press(fixture, 'ui-miniloan-010-assign-officer');
    const request = httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/assign`);
    expect(request.request.body).toEqual({ loanOfficerId: '' });
    request.flush(
      {
        code: 'LOAN_OFFICER_REQUIRED',
        message: 'มอบหมายไม่ได้ — ต้องระบุเจ้าหน้าที่สินเชื่อหนึ่งคน',
      },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.commandError()).toBe(
      'มอบหมายไม่ได้ — ต้องระบุเจ้าหน้าที่สินเชื่อหนึ่งคน',
    );
    // The queue did not reorganise itself around a command that failed.
    expect(all(fixture, 'ui-miniloan-010-ent-001-full-name')).toHaveLength(2);
  });

  /**
   * AC-miniloan-090's provable half, and the mirror of the assignment case above. BR-miniloan-047@v1
   * requires a reason and screens.json gives this screen no field to type one in, so the success path
   * — "ยกเลิกใบสมัครเรียบร้อย …" — cannot be reached from here at all. What is provable is that the
   * page sends the cancel and shows the API's own refusal.
   */
  it('has no field for a cancellation reason, and lets the API refuse the empty one', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-010-cancel-application');
    const request = httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/cancel`);
    expect(request.request.body).toEqual({ reason: '' });
    request.flush(
      { code: 'CANCELLATION_REASON_REQUIRED', message: 'ยกเลิกไม่ได้ — ต้องระบุเหตุผลการยกเลิก' },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.commandError()).toBe('ยกเลิกไม่ได้ — ต้องระบุเหตุผลการยกเลิก');
    expect(fixture.componentInstance.outcome()).toBeNull();
  });

  /**
   * AC-miniloan-091 — "การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ". The button is rendered and
   * fires whoever is looking at the page; the 403 is what proves the rule is enforced at the API. A
   * page that hid the button for a non-supervisor would make this criterion unmeasurable, which is
   * why the control is deliberately not gated on role.
   */
  it('sends the cancel for any caller and shows the API refusal for a non-supervisor', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-010-cancel-application');
    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1/cancel`).flush(
      {
        code: 'CANCEL_SUPERVISOR_ONLY',
        message: 'ยกเลิกใบสมัครที่ยังไม่ถูกมอบหมายได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ',
      },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.commandError()).toBe(
      'ยกเลิกใบสมัครที่ยังไม่ถูกมอบหมายได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ',
    );
    expect(all(fixture, 'ui-miniloan-010-cancel-application').length).toBeGreaterThan(0);
  });

  /**
   * AC-miniloan-092 reaching the page. Once an application is assigned the supervisor loses the right
   * to cancel it, and the sentence that says so is the API's. The rule itself is measured in
   * {@code LoanApplicationCancellationServiceTest}; what matters here is that the page prints the
   * refusal rather than removing the row as though the command had worked.
   */
  it('shows the handover refusal without touching the queue', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-010-cancel-application', 1);
    httpTesting.expectOne(`${API_BASE_URL}/applications/app-2/cancel`).flush(
      {
        code: 'CANCEL_ASSIGNED_OFFICER_ONLY',
        message: 'ใบสมัครนี้ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะเจ้าหน้าที่ที่รับผิดชอบใบนี้',
      },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.commandError()).toBe(
      'ใบสมัครนี้ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะเจ้าหน้าที่ที่รับผิดชอบใบนี้',
    );
    expect(all(fixture, 'ui-miniloan-010-ent-001-full-name')).toHaveLength(2);
  });

  /**
   * screens.json loading — only the button that was pressed is disabled, and only on ITS row. Two
   * rows carry the same testid, so a component that keyed busy on the command alone would freeze
   * every row's button at once and hide which application is being acted on.
   */
  it('disables only the pressed button on the row it belongs to', async () => {
    const fixture = await render();

    press(fixture, 'ui-miniloan-010-assign-officer');
    fixture.detectChanges();

    const assign = all(fixture, 'ui-miniloan-010-assign-officer');
    expect((assign[0] as HTMLButtonElement).disabled).toBe(true);
    expect((assign[1] as HTMLButtonElement).disabled).toBe(false);
    expect(
      (all(fixture, 'ui-miniloan-010-cancel-application')[0] as HTMLButtonElement).disabled,
    ).toBe(false);

    httpTesting
      .expectOne(`${API_BASE_URL}/applications/app-1/assign`)
      .flush({}, { status: 422, statusText: 'Unprocessable Entity' });
    fixture.detectChanges();
  });

  /** screens.json empty — every application is held, so the QUEUE is empty even though the list is not. */
  it('shows the declared empty sentence when every application is already assigned', async () => {
    const fixture = await render([row({ id: 'app-3', assignedLoanOfficerId: 'ROLE-002' })]);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ไม่มีใบสมัครรอมอบหมาย');
    expect(all(fixture, 'ui-miniloan-010-ent-001-full-name')).toHaveLength(0);
  });

  /**
   * screens.json unauthorized — "เมื่อ Loan Officer ทั่วไปพยายามเข้าถึงตาม BR-miniloan-031@v2". The
   * API's sentence is shown and no row of anyone's appears; error and unauthorized are separate
   * branches and only error offers a retry.
   */
  it('separates the API refusal from a failed load', async () => {
    const refused = TestBed.createComponent(UnassignedQueueComponent);
    refused.detectChanges();
    await refused.whenStable();
    httpTesting.expectOne(`${API_BASE_URL}/applications`).flush(
      { code: 'APPLICATION_LIST_FORBIDDEN', message: 'บทบาทนี้ไม่มีสิทธิ์ดูรายการใบสมัคร' },
      { status: 403, statusText: 'Forbidden' },
    );
    refused.detectChanges();

    expect(refused.componentInstance.notAllowed()).toBe('บทบาทนี้ไม่มีสิทธิ์ดูรายการใบสมัคร');
    expect(refused.componentInstance.failed()).toBe(false);
    expect(all(refused, 'ui-miniloan-010-ent-001-full-name')).toHaveLength(0);

    const broken = TestBed.createComponent(UnassignedQueueComponent);
    broken.detectChanges();
    await broken.whenStable();
    httpTesting
      .expectOne(`${API_BASE_URL}/applications`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    broken.detectChanges();

    expect(broken.componentInstance.failed()).toBe(true);
    expect(broken.componentInstance.notAllowed()).toBeNull();
    expect((broken.nativeElement as HTMLElement).textContent).toContain('โหลดรายการไม่สำเร็จ');
  });
});

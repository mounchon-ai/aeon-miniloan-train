import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PendingAdjustmentsListComponent } from './pending-adjustments-list.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { PendingAdjustment } from '../../core/services/closed-account-adjustment.service';

/**
 * UI-miniloan-013 (UC-miniloan-018 · ACL-016 · ACL-034).
 *
 * <p><b>What only a web test can prove.</b> That the queue holds nothing but Pending requests, and
 * that a caller who is not the configured approver is refused, are the API's questions and
 * AdjustmentControllerTest answers both. What is measured here is the half it cannot see: that the
 * page asks with the status design wrote into the call, that it renders every row the API returned
 * (ACL-034 is scope: all — there is no per-person scope to add), that the declared pagination slices
 * rows already received rather than re-asking the server, and that each row links to its own request.
 */
describe('PendingAdjustmentsListComponent (UI-miniloan-013)', () => {
  let httpTesting: HttpTestingController;

  const URL = `${API_BASE_URL}/adjustments?status=Pending`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PendingAdjustmentsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        // ui-miniloan-013-open-adjustment points at UI-miniloan-014 (screens.json destinationRef).
        provideRouter([{ path: 'adjustments/:id', children: [] }]),
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const row = (over: Partial<PendingAdjustment>): PendingAdjustment => ({
    id: 'adj-1',
    loanAccountId: 'acc-1',
    targetRecordId: 'pay-12',
    fieldName: 'Payment.amount',
    oldValue: '9583.33',
    newValue: '9583.30',
    requestedBy: 'ROLE-004',
    requestedAt: '2026-06-01T00:00:00Z',
    status: 'Pending',
    approvedBy: null,
    approvedAt: null,
    ...over,
  });

  const QUEUE: PendingAdjustment[] = [
    row({ id: 'adj-1', fieldName: 'Payment.amount', oldValue: '9583.33', newValue: '9583.30' }),
    row({
      id: 'adj-2',
      fieldName: 'LoanAccount.closedAt',
      oldValue: '2026-05-01T00:00:00Z',
      newValue: '2026-04-30T00:00:00Z',
      requestedBy: 'ROLE-002',
      requestedAt: '2026-06-03T00:00:00Z',
    }),
    row({ id: 'adj-3', fieldName: 'Payment.recordedAt', oldValue: 'A', newValue: 'B' }),
  ];

  function render(rows: PendingAdjustment[] = QUEUE) {
    const fixture = TestBed.createComponent(PendingAdjustmentsListComponent);
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      const request = httpTesting.expectOne(URL);
      request.flush(rows);
      fixture.detectChanges();
      return { fixture, request };
    });
  }

  const all = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelectorAll(`[data-testid="${id}"]`);

  const cells = (fixture: { nativeElement: unknown }, id: string) =>
    [...all(fixture, id)].map((cell) => cell.textContent?.trim());

  it('renders every control mock minted for this screen', async () => {
    const { fixture } = await render();

    for (const id of [
      'ui-miniloan-013-ent-010-field-name',
      'ui-miniloan-013-ent-010-old-value',
      'ui-miniloan-013-ent-010-new-value',
      'ui-miniloan-013-ent-010-requested-by',
      'ui-miniloan-013-ent-010-requested-at',
      'ui-miniloan-013-open-adjustment',
    ]) {
      expect(all(fixture, id).length).toBeGreaterThan(0);
    }
  });

  /**
   * ACL-016's condition is STM-miniloan-004 state Pending, and API-024 is declared as
   * `GET /adjustments?status=Pending`. The parameter is sent as design wrote the call — the route
   * refuses any other status, so a page that omitted it would be relying on a server default rather
   * than asking the question design asked.
   */
  it('asks with the status design wrote into the call', async () => {
    const { request } = await render();

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('status')).toBe('Pending');
  });

  /**
   * Every row the API returned, in the order it sent them, with each field in its own column — a
   * template that crossed oldValue with newValue would show the approver a change backwards.
   */
  it('renders every row the API returned, each value in its own field', async () => {
    const { fixture } = await render();

    expect(cells(fixture, 'ui-miniloan-013-ent-010-field-name')).toEqual([
      'Payment.amount',
      'LoanAccount.closedAt',
      'Payment.recordedAt',
    ]);
    expect(cells(fixture, 'ui-miniloan-013-ent-010-old-value')).toEqual([
      '9583.33',
      '2026-05-01T00:00:00Z',
      'A',
    ]);
    expect(cells(fixture, 'ui-miniloan-013-ent-010-new-value')).toEqual([
      '9583.30',
      '2026-04-30T00:00:00Z',
      'B',
    ]);
    expect(cells(fixture, 'ui-miniloan-013-ent-010-requested-by')).toEqual([
      'ROLE-004',
      'ROLE-002',
      'ROLE-004',
    ]);
  });

  /** The row's link is the request it belongs to, not a fixed one. */
  it('links each row to its own request', async () => {
    const { fixture } = await render();

    expect(
      [...all(fixture, 'ui-miniloan-013-open-adjustment')].map((link) => link.getAttribute('href')),
    ).toEqual(['/adjustments/adj-1', '/adjustments/adj-2', '/adjustments/adj-3']);
  });

  /** screens.json empty — "ไม่มีคำขอรออนุมัติ", word for word. */
  it('shows the declared empty sentence when the queue is clear', async () => {
    const { fixture } = await render([]);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ไม่มีคำขอรออนุมัติ');
    expect(all(fixture, 'ui-miniloan-013-ent-010-field-name')).toHaveLength(0);
  });

  /**
   * screens.json overflow — "แบ่งหน้า". The pager slices rows already received and issues NO second
   * request: the afterEach verify() is what proves it, and API-024 declares no page parameter, so a
   * page that asked the server for a page would be inventing one.
   */
  it('pages rows already received and never re-asks the API', async () => {
    const many = Array.from({ length: 23 }, (_, index) =>
      row({ id: `adj-${index + 1}`, newValue: String(index + 1) }),
    );
    const { fixture } = await render(many);

    expect(all(fixture, 'ui-miniloan-013-open-adjustment')).toHaveLength(20);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.pending-adjustments__pager'),
    ).not.toBeNull();

    fixture.componentInstance.goToPage(1);
    fixture.detectChanges();

    expect(cells(fixture, 'ui-miniloan-013-ent-010-new-value')).toEqual(['21', '22', '23']);
  });

  /**
   * A queue that fits on one page shows no pager. Asserted on the element rather than on the page
   * text, so a column heading that happens to contain the pager's wording cannot make it pass.
   */
  it('shows no pager when everything fits', async () => {
    const { fixture } = await render();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.pending-adjustments__pager'),
    ).toBeNull();
    expect(fixture.componentInstance.pageCount()).toBe(1);
  });

  /**
   * screens.json unauthorized — a caller who is not the configured approver. The API's own sentence
   * appears, and not one row of anybody's queue is left on screen.
   */
  it('shows the API refusal and no row when the caller is not the approver', async () => {
    const fixture = TestBed.createComponent(PendingAdjustmentsListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(URL).flush(
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
    expect(fixture.componentInstance.failed()).toBe(false);
    expect(all(fixture, 'ui-miniloan-013-ent-010-field-name')).toHaveLength(0);
  });

  /** screens.json error — the load failed, and a retry is offered rather than an empty queue. */
  it('offers a retry when the queue fails to load', async () => {
    const fixture = TestBed.createComponent(PendingAdjustmentsListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(URL).flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดรายการไม่สำเร็จ');

    ((fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement).click();
    httpTesting.expectOne(URL).flush(QUEUE);
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(false);
    expect(all(fixture, 'ui-miniloan-013-ent-010-field-name')).toHaveLength(3);
  });
});

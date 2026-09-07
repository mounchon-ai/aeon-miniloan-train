import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AssignedQueueComponent } from './assigned-queue.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { LoanApplication } from '../../core/services/loan-application.service';

/**
 * UI-miniloan-006 (UC-miniloan-005 · ACL-027).
 *
 * <p><b>What only a web test can prove.</b> That API-005 returns just the officer's assigned
 * applications is measured on the API side, where the rule lives — {@code ScopedListingServiceTest}
 * and {@code ScopedListingControllerTest} both assert the size with another officer's rows in the
 * table. What is measured here is the half those cannot see: that the page renders every row the
 * API returned and adds no scope of its own, that the declared status filter narrows rows already
 * received rather than re-asking the server with a filter the caller chose, and that a row with no
 * assessment still renders instead of being dropped.
 */
describe('AssignedQueueComponent (UI-miniloan-006)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssignedQueueComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        // ui-miniloan-006-open-application points at UI-miniloan-007 (screens.json destinationRef).
        provideRouter([{ path: 'review/:id', children: [] }]),
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const row = (over: Partial<LoanApplication>): LoanApplication => ({
    id: 'app-1',
    status: 'UnderReview',
    band: 'B',
    // FE-miniloan-024 made this a required field on the row; it is on the base object rather than
    // only in the override so the spread keeps the type string | null.
    assignedLoanOfficerId: 'ROLE-002',
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

  const QUEUE: LoanApplication[] = [
    row({ id: 'app-1', status: 'UnderReview', band: 'B', fullName: 'สมชาย ใจดี' }),
    row({ id: 'app-2', status: 'Approved', band: 'A', fullName: 'สมหญิง รักเรียน' }),
    row({ id: 'app-3', status: 'UnderReview', band: null, fullName: 'สมปอง มีสุข' }),
  ];

  function render(queue: LoanApplication[] = QUEUE) {
    const fixture = TestBed.createComponent(AssignedQueueComponent);
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      const request = httpTesting.expectOne(`${API_BASE_URL}/applications`);
      request.flush(queue);
      fixture.detectChanges();
      return { fixture, request };
    });
  }

  const all = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelectorAll(`[data-testid="${id}"]`);

  it('renders every control mock minted for this screen', async () => {
    const { fixture } = await render();

    for (const id of [
      'ui-miniloan-006-ent-001-full-name',
      'ui-miniloan-006-ent-002-status',
      'ui-miniloan-006-ent-003-band',
      'ui-miniloan-006-ent-002-requested-amount',
      'ui-miniloan-006-open-application',
      'ui-miniloan-006-filter-by-status',
    ]) {
      expect(all(fixture, id).length).toBeGreaterThan(0);
    }
  });

  /**
   * The page adds no scope. Every row the API sent is rendered — a template that hid, say, the
   * Approved one would still look like "the officer's queue" and would pass a weaker check.
   */
  it('renders every row the API returned, in the order it sent them', async () => {
    const { fixture } = await render();

    const names = [...all(fixture, 'ui-miniloan-006-ent-001-full-name')].map((cell) =>
      cell.textContent?.trim(),
    );
    expect(names).toEqual(['สมชาย ใจดี', 'สมหญิง รักเรียน', 'สมปอง มีสุข']);
  });

  /** ENT-003 is optional here: an application with no assessment keeps its row and shows no band. */
  it('keeps a row whose application has no assessment', async () => {
    const { fixture } = await render();

    const bands = [...all(fixture, 'ui-miniloan-006-ent-003-band')].map((cell) =>
      cell.textContent?.trim(),
    );
    expect(bands).toEqual(['B', 'A', '—']);
  });

  /**
   * screens.json declares filter-by-status as {@code kind: filter}, {@code destinationRef: self}.
   * Choosing a status narrows what is already on the page and issues NO second request — the
   * afterEach verify() would fail if one went out, which is the point: a filter that re-asked the
   * server would be the browser choosing its own scope (AC-miniloan-128).
   */
  it('filters rows already received and never re-asks the API', async () => {
    const { fixture } = await render();

    fixture.componentInstance.onStatusChange('Approved');
    fixture.detectChanges();

    const names = [...all(fixture, 'ui-miniloan-006-ent-001-full-name')].map((cell) =>
      cell.textContent?.trim(),
    );
    expect(names).toEqual(['สมหญิง รักเรียน']);
  });

  /** The filter offers only statuses the queue actually holds, so no choice can match nothing. */
  it('offers one option per status present, plus ทุกสถานะ', async () => {
    const { fixture } = await render();

    const select = (fixture.nativeElement as HTMLElement).querySelector(
      '[data-testid="ui-miniloan-006-filter-by-status"]',
    ) as HTMLSelectElement;
    expect([...select.options].map((option) => option.value)).toEqual([
      '',
      'Approved',
      'UnderReview',
    ]);
  });

  /** screens.json empty — "ไม่มีใบสมัครรอพิจารณา", word for word. */
  it('shows the declared empty sentence when nothing is assigned', async () => {
    const { fixture } = await render([]);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ไม่มีใบสมัครรอพิจารณา');
    expect(all(fixture, 'ui-miniloan-006-ent-001-full-name')).toHaveLength(0);
  });

  /** screens.json error — the load failed, and a retry is offered rather than an empty queue. */
  it('offers a retry when the queue fails to load', async () => {
    const fixture = TestBed.createComponent(AssignedQueueComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting
      .expectOne(`${API_BASE_URL}/applications`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดรายการไม่สำเร็จ');
  });
});

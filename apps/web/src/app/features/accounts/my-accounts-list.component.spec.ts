import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MyAccountsListComponent } from './my-accounts-list.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { LoanAccountSummary } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-011 (UC-miniloan-024 · ACL-020 · ACL-032).
 *
 * <p><b>What only a web test can prove.</b> That API-013 returns just the accounts assigned to the
 * caller is measured where the rule lives — LoanAccountScopeService and its controller tests both
 * assert the COUNT with somebody else's account in the table, which is AC-miniloan-137's point.
 * What is measured here is the half those cannot see: that the page renders every row the API
 * returned and adds no scope of its own, that the declared overflow behaviour pages rows already
 * received rather than re-asking the server, and that an account with nothing owing prints a dash
 * rather than instalment number zero.
 */
describe('MyAccountsListComponent (UI-miniloan-011)', () => {
  let httpTesting: HttpTestingController;

  const URL = `${API_BASE_URL}/loan-accounts`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyAccountsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        // ui-miniloan-011-open-account points at UI-miniloan-012 (screens.json destinationRef).
        provideRouter([{ path: 'loan-accounts/:id', children: [] }]),
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const row = (over: Partial<LoanAccountSummary>): LoanAccountSummary => ({
    accountNumber: 'acc-1',
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

  const QUEUE: LoanAccountSummary[] = [
    row({ accountNumber: 'acc-1', outstandingPrincipal: '70000.00', nextDueInstallmentNumber: 3 }),
    row({
      accountNumber: 'acc-2',
      status: 'Closed',
      outstandingPrincipal: '0.00',
      closedAt: '2026-05-01T00:00:00Z',
      closeReason: 'FullyPaid',
      nextDueInstallmentNumber: null,
    }),
    row({ accountNumber: 'acc-3', outstandingPrincipal: '15000.00', nextDueInstallmentNumber: 11 }),
  ];

  function render(rows: LoanAccountSummary[] = QUEUE) {
    const fixture = TestBed.createComponent(MyAccountsListComponent);
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
      'ui-miniloan-011-ent-006-status',
      'ui-miniloan-011-ent-006-outstanding-principal',
      'ui-miniloan-011-next-due-installment',
      'ui-miniloan-011-open-account',
    ]) {
      expect(all(fixture, id).length).toBeGreaterThan(0);
    }
  });

  /**
   * The page adds no scope. Every row the API sent is rendered, the Closed one included — a template
   * that hid it would still look like "the accounts I look after" and would pass a weaker check.
   */
  it('renders every row the API returned, in the order it sent them', async () => {
    const { fixture } = await render();

    expect(cells(fixture, 'ui-miniloan-011-ent-006-outstanding-principal')).toEqual([
      '70000.00',
      '0.00',
      '15000.00',
    ]);
    expect(cells(fixture, 'ui-miniloan-011-ent-006-status')).toEqual([
      'กำลังผ่อนชำระ (Active)',
      'ปิดบัญชีแล้ว (Closed)',
      'กำลังผ่อนชำระ (Active)',
    ]);
  });

  /**
   * null means nothing is owing and prints a dash; 0 would read as instalment number zero, which is
   * a row that does not exist. The API sends null for exactly that case and the two must not merge.
   */
  it('prints a dash, not a zero, for an account with nothing owing', async () => {
    const { fixture } = await render();

    expect(cells(fixture, 'ui-miniloan-011-next-due-installment')).toEqual(['3', '—', '11']);
  });

  /** The row's link is the account it belongs to, not a fixed one. */
  it('links each row to its own account', async () => {
    const { fixture } = await render();

    expect(
      [...all(fixture, 'ui-miniloan-011-open-account')].map((link) => link.getAttribute('href')),
    ).toEqual(['/loan-accounts/acc-1', '/loan-accounts/acc-2', '/loan-accounts/acc-3']);
  });

  /** screens.json empty — "ไม่มีบัญชีในความดูแล", word for word. */
  it('shows the declared empty sentence when nothing is assigned', async () => {
    const { fixture } = await render([]);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ไม่มีบัญชีในความดูแล');
    expect(all(fixture, 'ui-miniloan-011-ent-006-status')).toHaveLength(0);
  });

  /**
   * screens.json overflow — "แบ่งหน้า". The pager slices rows already received and issues NO second
   * request: the afterEach verify() is what proves it, and a page that re-asked the server with a
   * page number would be asking for a scope API-013 never declared.
   */
  it('pages rows already received and never re-asks the API', async () => {
    const many = Array.from({ length: 25 }, (_, index) =>
      row({ accountNumber: `acc-${index + 1}`, nextDueInstallmentNumber: index + 1 }),
    );
    const { fixture } = await render(many);

    expect(all(fixture, 'ui-miniloan-011-open-account')).toHaveLength(20);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.my-accounts__pager'),
    ).not.toBeNull();

    fixture.componentInstance.goToPage(1);
    fixture.detectChanges();

    expect(all(fixture, 'ui-miniloan-011-open-account')).toHaveLength(5);
    expect(cells(fixture, 'ui-miniloan-011-next-due-installment')).toEqual([
      '21',
      '22',
      '23',
      '24',
      '25',
    ]);
  });

  /**
   * A list that fits on one page shows no pager at all. Asserted on the element rather than on the
   * page text: the column heading is "งวดค้างถัดไป", so a text search for the pager's own label
   * matches the header and can never fail.
   */
  it('shows no pager when everything fits', async () => {
    const { fixture } = await render();

    expect((fixture.nativeElement as HTMLElement).querySelector('.my-accounts__pager')).toBeNull();
    expect(fixture.componentInstance.pageCount()).toBe(1);
  });

  /** screens.json error — the load failed, and a retry is offered rather than an empty list. */
  it('offers a retry when the list fails to load', async () => {
    const fixture = TestBed.createComponent(MyAccountsListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(URL).flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดรายการไม่สำเร็จ');
    // and it really re-reads
    (
      (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement
    ).click();
    httpTesting.expectOne(URL).flush(QUEUE);
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(false);
    expect(all(fixture, 'ui-miniloan-011-ent-006-status')).toHaveLength(3);
  });
});

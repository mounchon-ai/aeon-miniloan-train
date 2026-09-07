import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ApplicationDetailComponent } from './application-detail.component';
import { API_BASE_URL } from '../../core/api-base-url';

/**
 * UI-miniloan-003 (UC-miniloan-023 · AC-miniloan-127).
 *
 * <p><b>The unauthorized state is the one this screen owns.</b> screens.json declares it real here
 * and impossible on the other two, and AC-miniloan-127 says the refusal must be the API's:
 * "การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ไม่แสดงลิงก์บนหน้าจอ". FE-miniloan-019 made API-004 answer 403
 * with "ไม่มีสิทธิ์เข้าถึงใบสมัครนี้" and measured it there; what is proved here is that the page
 * asks for the id it was given, and renders the sentence that came back rather than one of its own.
 */
describe('ApplicationDetailComponent (UI-miniloan-003)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationDetailComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  const APPLICATION = {
    id: 'app-1',
    status: 'UnderReview',
    requestedAmount: '100000.00',
    requestedTermMonths: 12,
    submittedAt: '2026-09-01T10:00:00Z',
    rejectionReason: null,
  };

  const ASSESSMENT = {
    band: 'A',
    maxApprovableAmount: '150000.00',
    dtiRatio: '0.349900',
    dtiShown: '35.00%',
    reasons: ['อายุ 35 ปี ✓', 'รายได้ 30,000.00 บาท ✓'],
    assessedAt: '2026-09-01T10:00:01Z',
  };

  function render(id = 'app-1') {
    const fixture = TestBed.createComponent(ApplicationDetailComponent);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    return fixture;
  }

  const testid = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  /** Every control design declared for this screen, in the DOM where qa can select it. */
  it('renders every control mock minted for this screen', async () => {
    const fixture = render();
    await fixture.whenStable();

    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1`).flush({
      application: APPLICATION,
      assessment: ASSESSMENT,
      assessmentNote: null,
    });
    fixture.detectChanges();

    for (const id of [
      'ui-miniloan-003-ent-002-status',
      'ui-miniloan-003-ent-002-requested-amount',
      'ui-miniloan-003-ent-002-requested-term-months',
      'ui-miniloan-003-ent-002-submitted-at',
      'ui-miniloan-003-ent-003-band',
      'ui-miniloan-003-ent-003-max-approvable-amount',
      'ui-miniloan-003-ent-003-reasons',
      'ui-miniloan-003-ent-002-rejection-reason',
      'ui-miniloan-003-back-to-list',
    ]) {
      expect(testid(fixture, id)).not.toBeNull();
    }
  });

  /** The values are the API's, rendered as they arrived — no formatting decision is taken here. */
  it('renders the values the API returned', async () => {
    const fixture = render();
    await fixture.whenStable();

    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1`).flush({
      application: APPLICATION,
      assessment: ASSESSMENT,
      assessmentNote: null,
    });
    fixture.detectChanges();

    expect(testid(fixture, 'ui-miniloan-003-ent-002-status')?.textContent).toContain('UnderReview');
    expect(testid(fixture, 'ui-miniloan-003-ent-003-band')?.textContent).toContain('A');
    expect(testid(fixture, 'ui-miniloan-003-ent-003-reasons')?.textContent).toContain('อายุ 35 ปี ✓');
  });

  /**
   * AC-miniloan-127 — opening someone else's application by id. The page asks for the id it was
   * handed and shows the API's sentence; nothing here decides who owns what.
   */
  it('shows the API refusal when the application belongs to someone else', async () => {
    const fixture = render('someone-elses-id');
    await fixture.whenStable();

    httpTesting
      .expectOne(`${API_BASE_URL}/applications/someone-elses-id`)
      .flush(
        { code: 'APPLICATION_NOT_VISIBLE', message: 'ไม่มีสิทธิ์เข้าถึงใบสมัครนี้' },
        { status: 403, statusText: 'Forbidden' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.notAllowed()).toBe('ไม่มีสิทธิ์เข้าถึงใบสมัครนี้');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'ไม่มีสิทธิ์เข้าถึงใบสมัครนี้',
    );
    expect(fixture.componentInstance.detail()).toBeNull();
  });

  /**
   * A 403 is not the same page as a 500: screens.json gives unauthorized and error different
   * behaviour, and only error offers a retry.
   */
  it('separates a refusal from a failed load', async () => {
    const fixture = render();
    await fixture.whenStable();

    httpTesting
      .expectOne(`${API_BASE_URL}/applications/app-1`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(true);
    expect(fixture.componentInstance.notAllowed()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('โหลดรายละเอียดไม่สำเร็จ');
  });

  /** AC-miniloan-035's shape on this screen — a draft has no assessment and the API says so. */
  it('renders the API note instead of an assessment when there is none', async () => {
    const fixture = render();
    await fixture.whenStable();

    httpTesting.expectOne(`${API_BASE_URL}/applications/app-1`).flush({
      application: { ...APPLICATION, status: 'Draft', submittedAt: null },
      assessment: null,
      assessmentNote: 'ใบสมัครนี้ยังไม่ได้ยื่น จึงยังไม่มีผลการประเมิน',
    });
    fixture.detectChanges();

    expect(testid(fixture, 'ui-miniloan-003-ent-003-band')?.textContent).toContain(
      'ใบสมัครนี้ยังไม่ได้ยื่น จึงยังไม่มีผลการประเมิน',
    );
  });
});

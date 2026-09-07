import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
  API_UNREACHABLE_CODE,
  API_UNREACHABLE_MESSAGE,
  apiErrorInterceptor,
} from './api-error.interceptor';
import { ApiErrorService } from '../services/api-error.service';

/**
 * UC-miniloan-026 (AC-miniloan-119 · AC-miniloan-120 · BR-miniloan-028@v1).
 *
 * <p><b>What only a test at this level can prove.</b> Each screen already measures its own error
 * state against a refusal the API sent. What none of them can see is the case where the API sends
 * nothing at all — and the two things that then have to be true at once: that the raw English
 * transport error never reaches a screen, and that a real refusal is still delivered exactly as the
 * API worded it.
 *
 * <p>The second is the one that matters most. An interceptor that rewrote every failure would pass
 * any test that only checked the unreachable path, and it would silently override the API's sentence
 * on all ten screens — the sentences AC-miniloan-012, AC-miniloan-079 and a dozen others quote word
 * for word. So the pass-through is asserted as hard as the rewrite.
 */
describe('apiErrorInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let apiErrors: ApiErrorService;

  const URL = '/api/applications';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    apiErrors = TestBed.inject(ApiErrorService);
  });

  afterEach(() => httpTesting.verify());

  /** What a browser really hands back when nothing is listening on the other end. */
  function failTransport(url = URL) {
    httpTesting
      .expectOne(url)
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
  }

  /**
   * AC-miniloan-119. The browser's own message here is "Http failure response for /api/applications:
   * 0 Unknown Error" — the raw English string the criterion rules out — and what a screen receives
   * instead is the Thai sentence, in the same `message` field every screen in this app already reads.
   */
  it('turns an unreachable API into AC-miniloan-119 sentence, not the browser English', () => {
    let delivered: HttpErrorResponse | null = null;
    httpClient.get(URL).subscribe({ error: (error: HttpErrorResponse) => (delivered = error) });

    failTransport();

    const error = delivered as HttpErrorResponse | null;
    expect(error).not.toBeNull();
    expect((error!.error as { code: string; message: string }).message).toBe(
      'ตอนนี้เชื่อมต่อระบบไม่ได้ กรุณาลองใหม่อีกครั้ง',
    );
    expect((error!.error as { code: string }).code).toBe(API_UNREACHABLE_CODE);
    expect(error!.status).toBe(0);
    // the raw transport text is gone from what a screen would render
    expect((error!.error as { message: string }).message).not.toContain('Unknown Error');
    expect((error!.error as { message: string }).message).not.toContain('Http failure');
  });

  /** And the banner the shell renders carries the same sentence, from the same event. */
  it('raises the banner with the same sentence', () => {
    expect(apiErrors.message()).toBeNull();

    httpClient.get(URL).subscribe({ error: () => undefined });
    failTransport();

    expect(apiErrors.message()).toBe(API_UNREACHABLE_MESSAGE);
  });

  /**
   * <b>The half a lazy interceptor would break.</b> A 403 means the API is up and has spoken, and
   * BR-miniloan-025@v1 is why its sentence is the one that must reach the screen. This is the same
   * refusal FE-miniloan-028 renders on UI-miniloan-014; if this interceptor reworded it, that
   * criterion and nine others would quietly start showing the connectivity sentence instead.
   */
  it('delivers a real refusal exactly as the API worded it, and raises no banner', () => {
    let delivered: HttpErrorResponse | null = null;
    httpClient.get(URL).subscribe({ error: (error: HttpErrorResponse) => (delivered = error) });

    httpTesting.expectOne(URL).flush(
      {
        code: 'ADJUSTMENT_SELF_APPROVAL_REFUSED',
        message: 'อนุมัติคำขอของตัวเองไม่ได้ — ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้',
      },
      { status: 403, statusText: 'Forbidden' },
    );

    const error = delivered as HttpErrorResponse | null;
    expect(error!.status).toBe(403);
    expect((error!.error as { code: string; message: string })).toEqual({
      code: 'ADJUSTMENT_SELF_APPROVAL_REFUSED',
      message: 'อนุมัติคำขอของตัวเองไม่ได้ — ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้',
    });
    expect(apiErrors.message()).toBeNull();
  });

  /** The same for a 409 and a 500 — every status but 0 is the API answering. */
  it('passes every non-zero status through untouched', () => {
    for (const [status, body] of [
      [409, { code: 'LOAN_ACCOUNT_CLOSED', message: 'บัญชีนี้ปิดแล้ว — บันทึกการชำระเพิ่มไม่ได้' }],
      [422, { code: 'ADJUSTMENT_VALUE_UNREADABLE', message: 'ค่าใหม่อ่านไม่ได้' }],
      [500, { code: 'INTERNAL', message: 'ระบบขัดข้อง' }],
    ] as [number, { code: string; message: string }][]) {
      let delivered: HttpErrorResponse | null = null;
      httpClient.get(URL).subscribe({ error: (error: HttpErrorResponse) => (delivered = error) });
      httpTesting.expectOne(URL).flush(body, { status, statusText: 'x' });

      const error = delivered as HttpErrorResponse | null;
      expect(error!.status).toBe(status);
      expect((error!.error as { message: string }).message).toBe(body.message);
    }

    expect(apiErrors.message()).toBeNull();
  });

  /**
   * AC-miniloan-120, measured as a SEQUENCE in one running app: the call fails, the banner shows,
   * the page's own retry re-issues the same call, it succeeds, and the banner clears itself. No new
   * TestBed and no reload between the two — "ไม่ต้องปิดแล้วเปิดแอปใหม่ ไม่ต้องล็อกอินใหม่" is exactly
   * what that means, and calling clear() directly would prove nothing about the interceptor.
   */
  it('clears itself when the retry succeeds, with no reload in between', () => {
    httpClient.get(URL).subscribe({ error: () => undefined });
    failTransport();
    expect(apiErrors.message()).toBe(API_UNREACHABLE_MESSAGE);

    // the retry every screen already offers — the same call, nothing reinitialised
    let body: unknown = null;
    httpClient.get(URL).subscribe((response) => (body = response));
    httpTesting.expectOne(URL).flush([{ id: 'app-1' }]);

    expect(apiErrors.message()).toBeNull();
    expect(body).toEqual([{ id: 'app-1' }]);
  });

  /**
   * UC-miniloan-026's exception flow: "ผู้ใช้กดปุ่มลองใหม่ขณะ API ยังปิดให้บริการอยู่ — Web ยังคง
   * แสดงข้อผิดพลาดเช่นเดิมโดยไม่ค้างหรือ crash". The second failure leaves the same sentence, and the
   * error is delivered again rather than swallowed — a page that never heard back would be the
   * "ค้างหมุนตลอด" AC-miniloan-119 rules out.
   */
  it('keeps the same state when the retry fails too, and still answers the caller', () => {
    httpClient.get(URL).subscribe({ error: () => undefined });
    failTransport();

    let second: HttpErrorResponse | null = null;
    httpClient.get(URL).subscribe({ error: (error: HttpErrorResponse) => (second = error) });
    failTransport();

    expect(apiErrors.message()).toBe(API_UNREACHABLE_MESSAGE);
    expect(second).not.toBeNull();
    expect(((second as unknown as HttpErrorResponse).error as { message: string }).message).toBe(
      API_UNREACHABLE_MESSAGE,
    );
  });

  /**
   * BR-miniloan-028@v1 and REQ-miniloan-006: nothing here retries. A failed call surfaces at once
   * and the person re-issues it. The verify() in afterEach is what proves it — a retry policy would
   * show up as a second request nobody made.
   */
  it('issues exactly one request per call and never retries on its own', () => {
    httpClient.get(URL).subscribe({ error: () => undefined });

    const outstanding = httpTesting.match(URL);
    expect(outstanding).toHaveLength(1);
    outstanding[0].error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
  });
});

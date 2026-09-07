import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';

import { ApiErrorService } from '../services/api-error.service';

/**
 * UC-miniloan-026 · BR-miniloan-028@v1 — "Web ตรวจพบว่าเรียก API ไม่สำเร็จ … แสดงสถานะข้อผิดพลาด
 * อย่างเหมาะสมโดยไม่ crash".
 *
 * <p><b>AC-miniloan-119's sentence, in one place.</b> When the API is not answering at all, the
 * browser's own message is a raw English string — "Http failure response for …: 0 Unknown Error" —
 * and the criterion rules that out explicitly: "ไม่ใช่ข้อความภาษาอังกฤษดิบจากเบราว์เซอร์". So a
 * transport failure is rewritten here into an error carrying the Thai sentence in the same
 * {@code message} field every screen in this app already reads, and the banner raised beside it.
 * Ten screens get the criterion's wording without ten copies of it.
 *
 * <p><b>Every other status passes through untouched, and that is the important half.</b> A 403, a
 * 409 or a 422 means the API is working and has said something — BR-miniloan-025@v1 puts the
 * business rules there, and those sentences are what AC-miniloan-012, AC-miniloan-079,
 * AC-miniloan-088 and a dozen others quote word for word. An interceptor that rewrote them would
 * silently override the API on every screen at once, which is the one failure this unit could cause
 * and the reason its spec asserts the pass-through as hard as the rewrite.
 *
 * <p><b>Any answer clears the banner</b>, a refusal included (AC-miniloan-120). The page's own retry
 * button is what re-issues the call; nothing here retries, which REQ-miniloan-006 puts out of scope.
 */

/** AC-miniloan-119, word for word. */
export const API_UNREACHABLE_MESSAGE = 'ตอนนี้เชื่อมต่อระบบไม่ได้ กรุณาลองใหม่อีกครั้ง';

export const API_UNREACHABLE_CODE = 'API_UNREACHABLE';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const apiErrors = inject(ApiErrorService);

  return next(req).pipe(
    tap((event) => {
      if (event instanceof HttpResponse) {
        apiErrors.clear();
      }
    }),
    catchError((error: HttpErrorResponse) => {
      // status 0 is the transport failing: the API is down, unreachable, or refused the connection.
      // Anything else is the API answering, and its answer is not this interceptor's to reword.
      if (error.status !== 0) {
        apiErrors.clear();
        return throwError(() => error);
      }

      apiErrors.raise(API_UNREACHABLE_MESSAGE);
      return throwError(
        () =>
          new HttpErrorResponse({
            error: { code: API_UNREACHABLE_CODE, message: API_UNREACHABLE_MESSAGE },
            status: 0,
            statusText: error.statusText,
            url: error.url ?? undefined,
          }),
      );
    }),
  );
};

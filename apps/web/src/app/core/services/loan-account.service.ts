import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api-base-url';

/**
 * The loan-account calls apps/web makes, in one place.
 *
 * REQ-miniloan-006 · BR-miniloan-027@v1: nothing is computed here. The payoff figures in particular
 * are CALC-miniloan-004@v1's and are signed as GD-miniloan-005 — the browser asks for them and
 * renders what comes back, so what a borrower reads is the same number Operations would settle
 * against.
 */

/** apps/api RepaymentScheduleController.InstallmentResponse (ENT-008). */
export interface Installment {
  number: number;
  dueDate: string;
  emiAmount: string;
  interestPortion: string;
  principalPortion: string;
  remainingBalance: string;
  status: string;
}

/** apps/api RepaymentScheduleController.ScheduleResponse (ENT-007 + its instalments). */
export interface RepaymentSchedule {
  accountNumber: string;
  accountStatus: string;
  revisionNumber: number;
  issuedAt: string;
  principalAmount: string;
  outstandingPrincipal: string;
  termMonths: number;
  totalPrincipal: string;
  installments: Installment[];
}

/**
 * apps/api EarlyClosureController.PayoffQuoteResponse (API-015).
 *
 * {@code daysElapsed} travels because AC-miniloan-073 and AC-miniloan-075 quote it as part of what
 * the borrower is shown — "ดอกเบี้ยค้างจ่าย 10 วัน" — not as an internal step of the arithmetic.
 * {@code closingDate} is the server's echo of the date the quote was struck for.
 */
export interface PayoffQuote {
  closingDate: string;
  remainingPrincipal: string;
  daysElapsed: number;
  accruedInterest: string;
  earlySettlementFee: string;
  earlySettlementAmount: string;
}

@Injectable({ providedIn: 'root' })
export class LoanAccountService {
  private readonly http = inject(HttpClient);

  /** API-012 — ตารางผ่อนฉบับปัจจุบันของบัญชีหนึ่งบัญชี. */
  schedule(accountId: string): Observable<RepaymentSchedule> {
    return this.http.get<RepaymentSchedule>(`${API_BASE_URL}/loan-accounts/${accountId}/schedule`);
  }

  /**
   * API-015 — ยอดปิดบัญชีก่อนกำหนด ณ วันที่ขอ (AC-miniloan-071's "ณ วันที่ {วันที่ขอ}").
   *
   * The date is a parameter of the endpoint, so the caller has to supply one; today's date is the
   * request's own timestamp, not a business decision the browser took. What the page then DISPLAYS
   * as "วันที่คำนวณยอด" is {@link PayoffQuote.closingDate} — the server's echo — so the screen shows
   * the date the quote was actually struck for rather than the one it happened to ask with.
   */
  payoffQuote(accountId: string, closingDate: string): Observable<PayoffQuote> {
    return this.http.get<PayoffQuote>(`${API_BASE_URL}/loan-accounts/${accountId}/payoff-quote`, {
      params: new HttpParams().set('closingDate', closingDate),
    });
  }
}

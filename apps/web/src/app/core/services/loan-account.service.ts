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

/**
 * apps/api LoanAccountController.LoanAccountSummary (API-013 · API-023) — one row of
 * UI-miniloan-011 and the {@code account-summary} zone of UI-miniloan-012.
 *
 * The list and the single read hand back the same record, so what a row shows and what the detail
 * page shows can never disagree. Scope is the API's: BR-miniloan-054@v1 gives Operations the
 * accounts assigned to them, and this app never filters a second time on top.
 */
export interface LoanAccountSummary {
  accountNumber: string;
  applicationId: string;
  status: string;
  principalAmount: string;
  outstandingPrincipal: string;
  termMonths: number;
  disbursedAt: string;
  closedAt: string | null;
  closeReason: string | null;

  /**
   * UI-miniloan-011's next-due-installment. Design binds the field to no entity (gate 45) and mock
   * named it in conventions.json fieldMap[]; FE-miniloan-027 added it to API-013's row, computed by
   * the API from the current schedule's lowest Due instalment. Null means nothing is owing — a zero
   * would read as instalment number zero.
   */
  nextDueInstallmentNumber: number | null;
}

/**
 * apps/api PaymentController.PaymentResponse (API-014).
 *
 * <p>Every figure AC-miniloan-086's and AC-miniloan-013's sentences are built from travels here, and
 * the API's own note records that composing them is the screen's job. {@code principalReduction} is
 * the one that matters most: AC-miniloan-086's whole point is that the principal fell by 19,800.00
 * and not by the 20,000.00 that was handed over.
 */
export interface PaymentResult {
  paymentId: string;
  accountNumber: string;
  accountStatus: string;
  closeReason: string | null;
  closedAt: string | null;
  installmentNumber: number;
  installmentStatus: string;
  amount: string;
  paymentType: string;
  overpaymentAmount: string | null;
  prepaymentFee: string | null;
  principalReduction: string | null;
  outstandingPrincipal: string;
  reissuedRevisionNumber: number | null;
}

/** apps/api EarlyClosureController.SettlementResponse (API-016) — AC-miniloan-005's close. */
export interface SettlementResult {
  paymentId: string;
  loanAccountId: string;
  status: string;
  closeReason: string;
  closedAt: string;
  amountPaid: string;
  earlySettlementFee: string;
  cancelledInstallments: number[];
}

/** apps/api RepaymentScheduleController.RevisionResponse — AC-miniloan-008 · AC-miniloan-011. */
export interface ScheduleRevision {
  revisionNumber: number;
  issuedAt: string;
  supersededAt: string | null;
  current: boolean;
  totalPrincipal: string;
}

/** apps/api RepaymentScheduleController.RescheduleResponse (API-011). */
export interface RescheduleResult {
  accountNumber: string;
  accountStatus: string;
  revisionNumber: number;
  issuedAt: string;
  totalPrincipal: string;
  revisions: ScheduleRevision[];
  installments: Installment[];
}

/**
 * ENT-010's declared enum, copied from datamodel.json's {@code fieldName} — the five DECLARED names,
 * which is what apps/api deserialises and the only list a caller has been told about. Not invented
 * here and not read off the Java enum's constant names, which are a different spelling of the same
 * five.
 */
export const ADJUSTABLE_FIELDS = [
  'LoanAccount.closedAt',
  'LoanAccount.closeReason',
  'LoanAccount.assignedOperationsId',
  'Payment.amount',
  'Payment.recordedAt',
] as const;

/**
 * apps/api ClosedAccountAdjustmentController.AdjustmentResponse (API-017).
 *
 * {@code message} is AC-miniloan-076's "ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก
 * {role ผู้อนุมัติ}", worded by the API because the approver role is a row only it can read
 * (BR-miniloan-039@v1). The screen renders it; it never composes it.
 */
export interface AdjustmentResult {
  id: string;
  loanAccountId: string;
  targetRecordId: string;
  fieldName: string;
  oldValue: string;
  newValue: string;
  requestedBy: string;
  requestedAt: string;
  status: string;
  approverRole: string;
  message: string;
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

  /**
   * API-013 — บัญชีสินเชื่อที่ผู้เรียกเห็นได้ (UI-miniloan-011).
   *
   * No scope parameter and no filter: BR-miniloan-054@v1 is a WHERE clause on the server, and a
   * browser that narrowed the list a second time would be choosing its own scope.
   */
  list(): Observable<LoanAccountSummary[]> {
    return this.http.get<LoanAccountSummary[]>(`${API_BASE_URL}/loan-accounts`);
  }

  /** API-023 — บัญชีเดียว (UI-miniloan-012's account-summary zone). */
  detail(accountId: string): Observable<LoanAccountSummary> {
    return this.http.get<LoanAccountSummary>(`${API_BASE_URL}/loan-accounts/${accountId}`);
  }

  /**
   * API-014 — บันทึกการชำระหนึ่งงวด (UC-miniloan-012 · UC-miniloan-013).
   *
   * <p><b>The payment type is not sent</b>, and that is the API's own decision recorded in
   * {@code PaymentRequest}: BR-miniloan-019@v1 and BR-miniloan-046@v2 derive it from comparing the
   * amount with the instalment, and a caller able to label its own payment could label a short one
   * an overpayment. One method serves AC-miniloan-013 and AC-miniloan-086 alike.
   */
  recordPayment(
    accountId: string,
    installmentNumber: number,
    amount: string,
  ): Observable<PaymentResult> {
    return this.http.post<PaymentResult>(`${API_BASE_URL}/loan-accounts/${accountId}/payments`, {
      installmentNumber,
      amount,
    });
  }

  /**
   * API-016 — บันทึกการชำระยอดปิดบัญชีก่อนกำหนด (UC-miniloan-016 · AC-miniloan-005).
   *
   * {@code closingDate} is today's date, exactly as FE-miniloan-022's quote page sends it: the
   * endpoint takes a date and the request's own day is not a business decision the browser made.
   */
  recordPayoffPayment(
    accountId: string,
    closingDate: string,
    amount: string,
  ): Observable<SettlementResult> {
    return this.http.post<SettlementResult>(
      `${API_BASE_URL}/loan-accounts/${accountId}/payoff-settlement`,
      { closingDate, amount },
    );
  }

  /** API-011 — ออกตารางผ่อนฉบับใหม่ทับ (UC-miniloan-010). The endpoint declares no body. */
  reschedule(accountId: string): Observable<RescheduleResult> {
    return this.http.post<RescheduleResult>(
      `${API_BASE_URL}/loan-accounts/${accountId}/reschedule`,
      {},
    );
  }

  /** API-017 — ยื่นคำขอปรับปรุงบัญชีที่ปิดแล้ว (UC-miniloan-017 · ENT-010). */
  submitAdjustment(
    accountId: string,
    body: { targetRecordId: string; fieldName: string; oldValue: string; newValue: string },
  ): Observable<AdjustmentResult> {
    return this.http.post<AdjustmentResult>(
      `${API_BASE_URL}/loan-accounts/${accountId}/adjustments`,
      body,
    );
  }
}

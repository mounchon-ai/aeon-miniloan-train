import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api-base-url';

/**
 * Every call this feature makes to apps/api, in one place.
 *
 * REQ-miniloan-006 · BR-miniloan-027@v1: the web decides nothing. There is no arithmetic in this
 * file and no rule check — the shapes below mirror what the API returns and the components render
 * them. The ceiling on `previewMaxApprovable` is the clearest case: AC-miniloan-117 says the number
 * must come from the same side that decides at approval, so it is asked for on every change rather
 * than multiplied by five in the browser.
 *
 * The mock token rides on each request through `mockTokenInterceptor` (FE-miniloan-002), so nothing
 * here touches the Authorization header.
 */

/** apps/api LoanApplicationController.LoanApplicationResponse. */
export interface LoanApplication {
  id: string;
  status: string;
  fullName: string | null;
  age: number | null;
  monthlyIncome: string | null;
  currentEmploymentMonths: number | null;
  existingMonthlyDebt: string | null;
  requestedAmount: string | null;
  requestedTermMonths: number | null;
  approvedAmount: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  rejectedBy: string | null;
  rejectedAt: string | null;
  cancellationReason: string | null;
  cancelledBy: string | null;
  cancelledAt: string | null;
  submittedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** apps/api LoanApplicationController.CreditAssessmentResponse (ENT-003). */
export interface CreditAssessment {
  band: string;
  maxApprovableAmount: string;
  dtiRatio: string;
  dtiShown: string;
  reasons: string[];
  assessedAt: string;
}

/** apps/api LoanApplicationController.ApplicationDetailResponse. */
export interface ApplicationDetail {
  application: LoanApplication;
  assessment: CreditAssessment | null;
  assessmentNote: string | null;
}

/** apps/api LoanAccountController.LoanAccountSummary (API-013). */
export interface LoanAccount {
  accountNumber: string;
  applicationId: string;
  status: string;
  principalAmount: string;
  outstandingPrincipal: string;
  termMonths: number;
  disbursedAt: string | null;
  closedAt: string | null;
  closeReason: string | null;
}

/** What the applicant types. Every field may be blank while the application is still a draft. */
export interface DraftFields {
  fullName: string | null;
  age: number | null;
  monthlyIncome: string | null;
  currentEmploymentMonths: number | null;
  existingMonthlyDebt: string | null;
  requestedAmount: string | null;
  requestedTermMonths: number | null;
}

@Injectable({ providedIn: 'root' })
export class LoanApplicationService {
  private readonly http = inject(HttpClient);

  /** API-001 — บันทึกร่างใบสมัคร. */
  saveDraft(fields: DraftFields): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(`${API_BASE_URL}/applications`, fields);
  }

  /** API-002 — แก้ไขร่างใบสมัคร. */
  updateDraft(id: string, fields: DraftFields): Observable<LoanApplication> {
    return this.http.put<LoanApplication>(`${API_BASE_URL}/applications/${id}`, fields);
  }

  /** API-003 — ยื่นใบสมัคร. */
  submit(id: string): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>(`${API_BASE_URL}/applications/${id}/submit`, {});
  }

  /** API-005 — รายการใบสมัครที่ผู้เรียกมีสิทธิ์เห็น (scope decided by the API, never here). */
  listApplications(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(`${API_BASE_URL}/applications`);
  }

  /** API-004 — รายละเอียดใบสมัครหนึ่งใบ. */
  detail(id: string): Observable<ApplicationDetail> {
    return this.http.get<ApplicationDetail>(`${API_BASE_URL}/applications/${id}`);
  }

  /** API-013 — รายการบัญชีสินเชื่อที่ผู้เรียกมีสิทธิ์เห็น. */
  listAccounts(): Observable<LoanAccount[]> {
    return this.http.get<LoanAccount[]>(`${API_BASE_URL}/loan-accounts`);
  }

  /**
   * API-021 — วงเงินอนุมัติสูงสุดโดยประมาณ (BR-miniloan-003@v1 · BR-miniloan-027@v1).
   * AC-miniloan-117: called every time the income changes; the browser never multiplies by five.
   */
  previewMaxApprovable(monthlyIncome: string): Observable<{ maxApprovableAmount: string }> {
    return this.http.post<{ maxApprovableAmount: string }>(`${API_BASE_URL}/credit-assessments/preview`, {
      monthlyIncome,
    });
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api-base-url';

/**
 * API-020 — แดชบอร์ดภาพรวมสถานะ (UC-miniloan-020 · BR-miniloan-024@v1).
 *
 * <p>Its own file, like {@link ApproverRoleSettingService}: the summary belongs to no aggregate and
 * the counts are the API's answer, not a shape assembled from other calls.
 */

/**
 * apps/api DashboardSummaryService.DashboardSummary.
 *
 * <p><b>Five numbers from two aggregates, and the API decides all five.</b> The first three count
 * applications and the last two count loan accounts (AC-miniloan-110); nothing here adds, subtracts
 * or re-derives them. AC-miniloan-112 is the reason that matters: a Disbursed application already
 * has an Active account, and it is counted once as Active and never again under Approved — a browser
 * that summed anything would be the second place that decision lives.
 */
export interface DashboardSummary {
  submittedApplications: number;
  underReviewApplications: number;
  approvedApplications: number;
  activeLoanAccounts: number;
  closedLoanAccounts: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /** API-020 — the five counts, as one read. */
  summary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${API_BASE_URL}/dashboard`);
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api-base-url';

/**
 * API-017 · API-018 · API-024 · API-025 — the adjustment request once it has been filed
 * (ENT-010 · UC-miniloan-018 · ACL-016).
 *
 * <p>Its own file rather than a method on {@link LoanAccountService}: design roots all four of these
 * at `/adjustments` rather than under the account, and apps/api gave them their own controller for
 * the same reason. Filing a request is the other end of the story and lives on the account service,
 * where UI-miniloan-012 needs it.
 */

/**
 * apps/api AdjustmentController.AdjustmentView (API-024 · API-025).
 *
 * <p>`approvedBy` and `approvedAt` are null while the request is Pending — AC-miniloan-085's
 * premise, and the reason they travel as null rather than as a placeholder somebody could mistake
 * for a decision. Neither is a declared field on UI-miniloan-013 or UI-miniloan-014, so neither
 * carries a testid; see GAP-miniloan-010.
 */
export interface PendingAdjustment {
  id: string;
  loanAccountId: string;
  targetRecordId: string;
  fieldName: string;
  oldValue: string;
  newValue: string;
  requestedBy: string;
  requestedAt: string;
  status: string;
  approvedBy: string | null;
  approvedAt: string | null;
}

/**
 * apps/api AdjustmentController.DecisionResponse (API-017 · API-018).
 *
 * `message` is AC-miniloan-076's "อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ {เวลาอนุมัติ}" or
 * UC-miniloan-018's rejection sentence, and the service's own note records that it is built from the
 * value actually written to the row rather than from a second read of the clock. The screen renders
 * it; it never composes it.
 */
export interface AdjustmentDecision {
  adjustment: PendingAdjustment;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ClosedAccountAdjustmentService {
  private readonly http = inject(HttpClient);

  private readonly url = `${API_BASE_URL}/adjustments`;

  /**
   * API-024, declared as `GET /adjustments?status=Pending`. The parameter is sent as design wrote
   * the call, and Pending is the only value the route accepts: ACL-016's condition is
   * STM-miniloan-004 state Pending, so this is the QUEUE and not a history. What a history looks
   * like, and who may read it, is GAP-miniloan-010's open question.
   */
  listPending(): Observable<PendingAdjustment[]> {
    return this.http.get<PendingAdjustment[]>(this.url, {
      params: new HttpParams().set('status', 'Pending'),
    });
  }

  /** API-025 — one request, with both values side by side for the person deciding. */
  view(adjustmentId: string): Observable<PendingAdjustment> {
    return this.http.get<PendingAdjustment>(`${this.url}/${adjustmentId}`);
  }

  /** API-017 — the second of UC-miniloan-018's two steps, and the only one that changes data. */
  approve(adjustmentId: string): Observable<AdjustmentDecision> {
    return this.http.post<AdjustmentDecision>(`${this.url}/${adjustmentId}/approve`, {});
  }

  /** API-018 — UC-miniloan-018's alternate flow; the old value stays on the account. */
  reject(adjustmentId: string): Observable<AdjustmentDecision> {
    return this.http.post<AdjustmentDecision>(`${this.url}/${adjustmentId}/reject`, {});
  }
}

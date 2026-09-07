import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api-base-url';

/**
 * API-019 and API-022 — ตั้งค่าผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้ว (ENT-011 · UC-miniloan-019).
 *
 * <p>Its own file rather than a method on {@link LoanApplicationService}: ENT-011 is a system setting
 * with a single row and nothing to do with an application's life cycle, and the two would only share
 * a base URL.
 */

/**
 * ENT-011's declared enum. The three values are copied from datamodel.json, which is where design
 * declares them — not from the API's Java enum and not invented here.
 *
 * <p><b>They have no Thai label anywhere in design, and none is invented.</b> rbac.json's Thai labels
 * belong to ROLE-001..005, which is a different list; apps/api's service records the same refusal in
 * its own note. The screen shows the value as design spells it until somebody names them.
 */
export const APPROVER_ROLES = ['LoanOfficer', 'Supervisor', 'Operations'] as const;

export type ApproverRole = (typeof APPROVER_ROLES)[number];

/**
 * apps/api ApproverRoleSettingController.ApproverRoleSettingView (API-022).
 * A null {@code approverRole} is BR-miniloan-040@v1's "ยังไม่ได้ตั้ง" — a state of the system that a
 * caller has to be able to see, which is why the route answers with it instead of a 404.
 */
export interface ApproverRoleSettingView {
  approverRole: string | null;
  updatedBy: string | null;
  updatedAt: string | null;
}

/**
 * apps/api ApproverRoleSettingController.ApproverRoleSettingResponse (API-019).
 * {@code message} is AC-miniloan-082's sentence or AC-miniloan-089's, chosen by the API from whether
 * the row already existed — a fact about the row, so the browser must not decide it.
 */
export interface ApproverRoleSettingResult {
  approverRole: string;
  updatedBy: string;
  updatedAt: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ApproverRoleSettingService {
  private readonly http = inject(HttpClient);

  private readonly url = `${API_BASE_URL}/settings/closed-account-approver-role`;

  /** API-022 — อ่านค่าที่ตั้งไว้ (และสถานะ "ยังไม่ได้ตั้ง"). */
  view(): Observable<ApproverRoleSettingView> {
    return this.http.get<ApproverRoleSettingView>(this.url);
  }

  /** API-019 — บันทึกหรือเปลี่ยน role ผู้อนุมัติ (BR-miniloan-039@v1 · BR-miniloan-048@v1). */
  set(approverRole: string): Observable<ApproverRoleSettingResult> {
    return this.http.put<ApproverRoleSettingResult>(this.url, { approverRole });
  }
}

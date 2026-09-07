import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import {
  APPROVER_ROLES,
  ApproverRoleSettingService,
} from '../../core/services/approver-role-setting.service';

/**
 * UI-miniloan-008 · ตั้งค่าผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้ว (UC-miniloan-019 · ACL-029).
 *
 * <p><b>Every sentence on this page after a save is the API's</b> (AC-miniloan-082 · AC-miniloan-089).
 * The two criteria word the confirmation differently — "ตั้งค่า … เรียบร้อย" the first time and
 * "เปลี่ยน role ผู้อนุมัติเป็น … เรียบร้อย" afterwards — and which one applies depends on whether the
 * row already existed, a fact only the API can see. It returns the chosen sentence in
 * {@code message} and the page renders it; a browser that picked between them would be deciding from
 * what it happens to have loaded, and would say "ตั้งค่า" to somebody who had just changed it.
 *
 * <p><b>The three roles are design's list, and they have no Thai label.</b> datamodel.json declares
 * ENT-011's {@code approverRole} as an enum of LoanOfficer · Supervisor · Operations. Nothing in
 * design names them in Thai — rbac.json's Thai labels are ROLE-001..005, a different list — so the
 * value is shown as design spells it rather than translated here.
 *
 * <p><b>AC-miniloan-083 is enforced by the API, and this page does not help.</b> "ทั้งจากหน้าจอ และ
 * ด้วยการเรียก API ตั้งค่าโดยตรง … ทั้งสองทางถูกปฏิเสธ" — so the read is issued for whoever opens the
 * page and the API's own sentence is what appears. The criterion's other half, that the menu does not
 * show for these roles, is already true: sitemap.json marks UI-miniloan-008 {@code entry: false}, so
 * FE-miniloan-001's nav links no role to it at all.
 *
 * <p><b>screens.json's empty state is the one that matters most.</b> "ยังไม่เคยตั้งค่ามาก่อน — แสดง
 * ค่าว่างพร้อมข้อความเตือนว่าคำขอปรับปรุงบัญชีจะถูกปฏิเสธทั้งหมดจนกว่าจะตั้งค่า" (BR-miniloan-040@v1).
 * That is the state the whole adjustment feature is gated behind, and a page that showed a blank
 * select with no warning would look identical to one where somebody had simply not chosen yet.
 */
@Component({
  selector: 'app-approver-role-setting',
  imports: [ReactiveFormsModule],
  templateUrl: './approver-role-setting.component.html',
  styleUrl: './approver-role-setting.component.scss',
})
export class ApproverRoleSettingComponent {
  private readonly service = inject(ApproverRoleSettingService);

  readonly roles = APPROVER_ROLES;

  readonly approverRole = new FormControl('', { nonNullable: true });

  /** The value in force, as the API last reported it — never what the select happens to hold. */
  readonly current = signal<string | null>(null);
  readonly updatedBy = signal<string | null>(null);
  readonly updatedAt = signal<string | null>(null);

  readonly loaded = signal(false);
  readonly saving = signal(false);

  /** screens.json unauthorized — the API's own sentence, for the read and for the write alike. */
  readonly notAllowed = signal<string | null>(null);

  /** screens.json error — the save failed; the chosen value is deliberately left in the control. */
  readonly failed = signal<string | null>(null);

  /** AC-miniloan-082 · AC-miniloan-089's sentence, exactly as the API worded it. */
  readonly outcome = signal<string | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.notAllowed.set(null);

    this.service.view().subscribe({
      next: (view) => {
        this.current.set(view.approverRole);
        this.updatedBy.set(view.updatedBy);
        this.updatedAt.set(view.updatedAt);
        // An unset system leaves the control empty on purpose — see isUnset().
        this.approverRole.setValue(view.approverRole ?? '');
        this.loaded.set(true);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 403) {
          this.notAllowed.set(this.messageOf(error));
        } else {
          this.failed.set(this.messageOf(error));
        }
        this.loaded.set(true);
      },
    });
  }

  /**
   * API-019. The value is sent as chosen, empty included: BR-miniloan-039@v1 and BR-miniloan-048@v1
   * are the API's rules, and AC-miniloan-083 requires the refusal to happen there rather than by this
   * page declining to call.
   */
  save(): void {
    this.saving.set(true);
    this.failed.set(null);
    this.outcome.set(null);

    this.service.set(this.approverRole.value).subscribe({
      next: (result) => {
        this.current.set(result.approverRole);
        this.updatedBy.set(result.updatedBy);
        this.updatedAt.set(result.updatedAt);
        this.outcome.set(result.message);
        this.saving.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 403) {
          this.notAllowed.set(this.messageOf(error));
        } else {
          // screens.json: "คงค่าที่เลือกไว้" — the control is not reset, so a retry does not make
          // the person choose again.
          this.failed.set(this.messageOf(error));
        }
        this.saving.set(false);
      },
    });
  }

  /** BR-miniloan-040@v1's gate: nothing is set, so every adjustment request is refused. */
  isUnset(): boolean {
    return this.loaded() && !this.notAllowed() && this.current() === null;
  }

  private messageOf(error: HttpErrorResponse): string {
    const body = error.error as { message?: string } | null;
    return body?.message ?? 'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง';
  }
}

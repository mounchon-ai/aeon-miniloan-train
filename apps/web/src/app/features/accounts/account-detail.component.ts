import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';

import { RecordPaymentComponent } from './record-payment.component';
import { SubmitAdjustmentComponent } from './submit-adjustment.component';
import {
  Installment,
  LoanAccountService,
  LoanAccountSummary,
  RepaymentSchedule,
  ScheduleRevision,
} from '../../core/services/loan-account.service';

/**
 * UI-miniloan-012 · รายละเอียดบัญชีสินเชื่อ (UC-miniloan-010 · 012 · 013 · 016 · 017 · ACL-020 ·
 * ACL-033 · ROLE-004).
 *
 * <p>Four zones, and this component owns two of them — `account-summary` and `schedule-table`. The
 * other two are their own components because each is a command with its own form, its own busy
 * state and its own refusal: `payment-form` and `adjustment-form`.
 *
 * <p><b>The schedule read is ACL-020's, and FE-miniloan-027 is what made it possible.</b> API-012
 * answered ACL-010 alone until this unit — ROLE-001, the owning Applicant — even though ACL-020
 * grants Operations "ดูและดำเนินการกับบัญชีสินเชื่อที่ตนถูก assign" and ACL-033 puts this screen's
 * `schedule-table` zone in front of them. The branch was added in apps/api, against ACL-020's own
 * scope; see RepaymentScheduleQueryService#viewAsAssignedOperations.
 *
 * <p><b>Closing the account is not a control on this page.</b> AC-miniloan-006 and AC-miniloan-069
 * both speak of a "ปิดบัญชี" button, but screens.json declares four actions on this screen and none
 * of them is one, so mock minted no id for it — and gate 37 forbids inventing one. What the API does
 * hold is `POST /loan-accounts/{id}/close`, whose only behaviour is to refuse (AC-miniloan-006 ·
 * AC-miniloan-007), and that refusal is measured in PaymentControllerTest. The absence is asserted
 * here and carried up as a gap card, exactly as AC-miniloan-072's close button was on
 * UI-miniloan-005.
 *
 * <p><b>The revision list is half-buildable, and the built half is here.</b> AC-miniloan-008 and
 * AC-miniloan-011 want "ฉบับที่ 2 (ใช้อยู่)", the superseded revisions beside it, and a
 * "ดูฉบับก่อนหน้า" control that opens revision 1's rows. The current revision's own line renders
 * from API-012 on every load. The list of revisions renders from API-011's `revisions[]` — which
 * carries `supersededAt` and `current` — but only after a reschedule in this session, because no
 * endpoint returns that list on a plain read. Opening a superseded revision's ROWS has neither an
 * endpoint (`installments[]` is the current revision only) nor a minted control. Both halves are
 * carried up as gap cards rather than guessed at.
 */
@Component({
  selector: 'app-account-detail',
  imports: [RecordPaymentComponent, SubmitAdjustmentComponent],
  templateUrl: './account-detail.component.html',
  styleUrl: './account-detail.component.scss',
})
export class AccountDetailComponent {
  private readonly service = inject(LoanAccountService);

  readonly id = input.required<string>();

  readonly account = signal<LoanAccountSummary | null>(null);
  readonly schedule = signal<RepaymentSchedule | null>(null);

  /** Only ever what API-011 handed back in this session — see the class note. */
  readonly revisions = signal<ScheduleRevision[]>([]);

  readonly loading = signal(true);

  /** screens.json unauthorized — the API's own sentence (BR-miniloan-054@v1). */
  readonly notAllowed = signal<string | null>(null);

  /** screens.json error — per cause, in the API's words. */
  readonly failed = signal<string | null>(null);

  /** The schedule zone alone can fail while the summary loaded, so it carries its own message. */
  readonly scheduleFailed = signal<string | null>(null);

  readonly rescheduling = signal(false);
  readonly rescheduleFailed = signal<string | null>(null);

  /**
   * ACL-011 · ACL-012's own condition: the payment targets a Due instalment, and instalments are
   * retired in order (BR-miniloan-020@v1), so the earliest Due row is the one owing.
   */
  readonly nextDueInstallment = computed<number | null>(() => {
    const due = this.schedule()?.installments.find((row) => row.status === 'Due');
    return due ? due.number : null;
  });

  /** ACL-015's condition: an adjustment is filed against a CLOSED account and no other. */
  readonly closed = computed(() => this.account()?.status === 'Closed');

  constructor() {
    // input.required() is not readable in the constructor; the house idiom defers one tick.
    queueMicrotask(() => this.load());
  }

  load(): void {
    this.loading.set(true);
    this.notAllowed.set(null);
    this.failed.set(null);
    this.scheduleFailed.set(null);

    this.service.detail(this.id()).subscribe({
      next: (account) => {
        this.account.set(account);
        this.loading.set(false);
        this.loadSchedule();
      },
      error: (error: HttpErrorResponse) => {
        this.account.set(null);
        this.schedule.set(null);
        if (error.status === 403) {
          this.notAllowed.set(this.messageOf(error));
        } else {
          this.failed.set(this.messageOf(error));
        }
        this.loading.set(false);
      },
    });
  }

  private loadSchedule(): void {
    this.service.schedule(this.id()).subscribe({
      next: (schedule) => {
        this.schedule.set(schedule);
        this.scheduleFailed.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.schedule.set(null);
        this.scheduleFailed.set(this.messageOf(error));
      },
    });
  }

  /**
   * API-011. AC-miniloan-107 · AC-miniloan-108 · AC-miniloan-109: a Closed account refuses, and the
   * refusal is the API's — this page does not hide the button when the account is closed, because
   * AC-miniloan-109's point is that the permission goes away with the closing and BR-miniloan-025@v1
   * requires that to be enforced where the rule lives.
   */
  reschedule(): void {
    this.rescheduling.set(true);
    this.rescheduleFailed.set(null);

    this.service.reschedule(this.id()).subscribe({
      next: (result) => {
        this.revisions.set(result.revisions);
        this.rescheduling.set(false);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.rescheduleFailed.set(this.messageOf(error));
        this.rescheduling.set(false);
      },
    });
  }

  /** ENT-006's two states, in AC-miniloan-004's and AC-miniloan-006's own words. */
  statusLabel(status: string): string {
    return status === 'Closed' ? 'ปิดบัญชีแล้ว (Closed)' : 'กำลังผ่อนชำระ (Active)';
  }

  /**
   * AC-miniloan-004 and AC-miniloan-005 word the closing line differently and the difference is
   * `closeReason`, which is a fact about the account rather than about the payment that produced it —
   * so it is rendered from the summary and not from either command's response. The two are ENT-006's
   * only close reasons, and BR-miniloan-021@v1 says there is no third way.
   */
  closingSentence(): string | null {
    const account = this.account();
    if (!account || account.status !== 'Closed') {
      return null;
    }
    return account.closeReason === 'EarlySettlement'
      ? `ปิดบัญชีก่อนกำหนด — ชำระยอดปิดบัญชีครบเมื่อ ${account.closedAt}`
      : `ชำระครบทุกงวดแล้ว — ปิดบัญชีเมื่อ ${account.closedAt}`;
  }

  /** STM-miniloan-003's three states in the words AC-miniloan-004 and AC-miniloan-005 use. */
  installmentLabel(row: Installment): string {
    if (row.status === 'Paid') {
      return 'จ่ายแล้ว';
    }
    return row.status === 'Cancelled' ? 'ยกเลิก (ปิดบัญชีก่อนกำหนด)' : 'ค้าง';
  }

  private messageOf(error: HttpErrorResponse): string {
    const body = error.error as { message?: string } | null;
    return body?.message ?? 'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง';
  }
}

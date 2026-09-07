import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import {
  ApplicationDetail,
  LoanApplicationService,
} from '../../core/services/loan-application.service';

/** The four commands screens.json declares in the decision zone. */
type Command = 'approve' | 'reject' | 'cancel' | 'disburse';

/**
 * UI-miniloan-007 · พิจารณาใบสมัคร (UC-miniloan-005 · 006 · 007 · 009).
 *
 * <p><b>Every rule is the API's.</b> Nothing here checks a status, compares an amount against the
 * ceiling, or decides whether a reason is required: the page sends the command and renders the
 * sentence that comes back (REQ-miniloan-006 · BR-miniloan-025@v1). AC-miniloan-062 is the reason —
 * "การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ" — so a browser-side guard that stopped the
 * call would hide the very refusal the criterion measures. The blank-reason cases (AC-miniloan-056 ·
 * AC-miniloan-068) are sent and refused by the API for exactly that reason.
 *
 * <p><b>One call per press, and nothing behind it</b> (AC-miniloan-131 · AC-miniloan-132 ·
 * BR-miniloan-042@v1). A failure sets an error and stops: there is no retry, no queue and no
 * spinner left turning. The status shown after a command comes from that command's own response
 * rather than a second read, so a press is one request and the page cannot be the place a duplicate
 * disbursement is born.
 *
 * <p><b>screens.json's loading state, precisely</b> — "ปุ่มที่กดถูก disable และแสดง spinner ป้องกัน
 * กดซ้ำ" (BR-miniloan-043@v1). {@link busy} holds which command is in flight, so only that button is
 * disabled and the others stay readable.
 *
 * <p><b>Two things design asks for that this screen cannot do, and does not fake.</b>
 *
 * <ul>
 *   <li>AC-miniloan-054 has the officer lower the approved amount to 150,000 and approve again, but
 *       screens.json declares only two capture fields on this screen (reject and cancel reasons) and
 *       mock minted no id for an amount input. Approve therefore sends no body, which API-007 reads
 *       as "at the amount that was requested". Inventing the input would be a control nobody asked
 *       for (DV17 Class B) carrying a testid nobody minted (gate 37) — it is a card for design.
 *   <li>The cancel action's {@code destinationRef} is UI-miniloan-006, but AC-miniloan-047 requires
 *       this screen to show "ยกเลิกใบสมัครเรียบร้อย …" with the approve, reject and disburse buttons
 *       gone. The criterion is what gets measured, so the message stays here and the page does not
 *       navigate away from it.
 * </ul>
 */
@Component({
  selector: 'app-application-review',
  imports: [ReactiveFormsModule],
  templateUrl: './application-review.component.html',
  styleUrl: './application-review.component.scss',
})
export class ApplicationReviewComponent {
  private readonly service = inject(LoanApplicationService);

  /** Bound from the route by `withComponentInputBinding()` (app.config.ts). */
  readonly id = input.required<string>();

  readonly detail = signal<ApplicationDetail | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  /** screens.json unauthorized — the API's own sentence, never one written here. */
  readonly notAllowed = signal<string | null>(null);

  /** screens.json error — whatever the API refused with, shown without changing the status. */
  readonly commandError = signal<string | null>(null);

  /** The sentence after a command succeeded (AC-047 · 049 · 055 · 058 · 067). */
  readonly outcome = signal<string | null>(null);

  readonly busy = signal<Command | null>(null);

  readonly rejectionReason = new FormControl('', { nonNullable: true });
  readonly cancellationReason = new FormControl('', { nonNullable: true });

  /**
   * The status the page is showing — the loaded application's, or the one the last command answered
   * with. Never derived from which button was pressed: only the API says what an application became.
   */
  readonly status = signal<string | null>(null);

  /** AC-miniloan-047 — once cancelled, approve, reject and disburse are gone, not merely disabled. */
  readonly decidable = computed(() => {
    const status = this.status();
    return status !== null && status !== 'Cancelled';
  });

  constructor() {
    // `id` is a required input and is not readable inside the constructor — the same deferral
    // FE-miniloan-022's two screens use, so the first load happens once the route has bound it.
    queueMicrotask(() => this.load());
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.notAllowed.set(null);

    this.service.detail(this.id()).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.status.set(detail.application.status);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.detail.set(null);
        this.status.set(null);
        if (error.status === 403) {
          this.notAllowed.set(this.messageOf(error));
        } else {
          this.failed.set(true);
        }
        this.loading.set(false);
      },
    });
  }

  approve(): void {
    this.run('approve', this.service.approve(this.id()), (result) =>
      `อนุมัติโดย ${result.approvedBy} เมื่อ ${result.approvedAt}`,
    );
  }

  reject(): void {
    this.run('reject', this.service.reject(this.id(), this.rejectionReason.value), (result) =>
      `ปฏิเสธโดย ${result.rejectedBy} เมื่อ ${result.rejectedAt} · เหตุผล: ${result.rejectionReason}`,
    );
  }

  cancel(): void {
    this.run('cancel', this.service.cancel(this.id(), this.cancellationReason.value), (result) =>
      'ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง · ' +
      `ยกเลิกโดย ${result.cancelledBy} เมื่อ ${result.cancelledAt} · เหตุผล: ${result.cancellationReason}`,
    );
  }

  disburse(): void {
    this.run('disburse', this.service.disburse(this.id()), (result) =>
      `เบิกจ่ายเรียบร้อย — เปิดบัญชีสินเชื่อเลขที่ ${result.accountNumber}`,
    );
  }

  /**
   * One command, one subscription, no retry (AC-miniloan-131). The new status is read off the
   * response — every one of the four routes returns it — so a success costs exactly one request.
   */
  private run<T extends { status?: string; applicationStatus?: string }>(
    command: Command,
    call: Observable<T>,
    sentence: (result: T) => string,
  ): void {
    this.busy.set(command);
    this.commandError.set(null);
    this.outcome.set(null);

    call.subscribe({
      next: (result) => {
        this.status.set(result.status ?? result.applicationStatus ?? this.status());
        this.outcome.set(sentence(result));
        this.busy.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.commandError.set(this.messageOf(error));
        this.busy.set(null);
      },
    });
  }

  /**
   * AC-miniloan-131's sentence is this app's only invented message, and it exists because a timeout
   * has no response body to quote — status 0 is the browser giving up, not the API answering.
   */
  private messageOf(error: HttpErrorResponse): string {
    const body = error.error as { message?: string } | null;
    return (
      body?.message ?? 'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง'
    );
  }
}

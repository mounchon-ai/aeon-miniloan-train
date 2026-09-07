import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';

import { CurrentRoleService } from '../../core/services/current-role.service';
import {
  ClosedAccountAdjustmentService,
  PendingAdjustment,
} from '../../core/services/closed-account-adjustment.service';

/**
 * UI-miniloan-014 · พิจารณาคำขอปรับปรุงบัญชีที่ปิดแล้ว (UC-miniloan-018 · ACL-016 · ACL-035 ·
 * ROLE-005).
 *
 * <p><b>AC-miniloan-079's screen half is the opposite of the ones this app has met before.</b>
 * AC-miniloan-070 and AC-miniloan-083 said the control must not be THERE; this criterion says it is
 * there, in a state that cannot be pressed — "ปุ่ม 'อนุมัติ' บนคำขอที่ตัวเองยื่นแสดงเป็นสถานะกดไม่ได้".
 * So the approve button is rendered and disabled when the person viewing filed the request, which is
 * a comparison this page can make: under the mock scheme the resolved ROLE id doubles as the person
 * identity, and it is the same string {@code SelfApprovalRefusedException} compares on the server.
 *
 * <p><b>The disabled button does NOT replace the API's refusal.</b> AC-miniloan-079 measures both
 * ways — "ทั้งจากหน้าจอ และด้วยการเรียก API อนุมัติโดยตรง … ทั้งสองทางถูกปฏิเสธ" — and
 * BR-miniloan-025@v1 puts the rule at the API. The server's sentence is rendered untouched when it
 * comes back, and this page never composes THAT sentence: screens.json's error state does ask this
 * screen to say why the approval is barred, and with the button disabled the API is never called, so
 * the line beside it is written here — but it REFERENCES BR-miniloan-049@v1 rather than restating
 * the rule's wording, so the sentence itself keeps one home (W3).
 *
 * <p><b>Both buttons are also disabled once the request is decided.</b> That is ACL-016's own
 * condition — {@code state: ["Pending"]} on STM-miniloan-004 — not a rule this screen added, and the
 * API refuses a second decision anyway with "คำขอปรับปรุงนี้ถูกพิจารณาไปแล้ว — พิจารณาซ้ำไม่ได้".
 *
 * <p><b>Reject stays enabled on one's own request, deliberately.</b> BR-miniloan-049@v1 and the
 * API's refusal are about APPROVAL. Disabling reject as well would be a rule this unit invented, and
 * it would look identical to one design had written.
 *
 * <p><b>Both outcome sentences are the API's.</b> {@code DecisionResult#message} builds
 * AC-miniloan-076's "อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ {เวลาอนุมัติ}" from the value
 * actually written to the row, and its Javadoc says so — "not from a second read of the clock". A
 * page that assembled that time from its own {@code approvedAt} would be reading the clock twice.
 *
 * <p><b>What this screen cannot show, and the card that already covers it.</b> AC-miniloan-084 wants
 * five values including ผู้อนุมัติ and เวลาอนุมัติ, and AC-miniloan-085 wants a per-account history
 * sentence that counts pending requests. screens.json declares five display fields on this screen and
 * neither `approvedBy` nor `approvedAt` is among them, so mock minted no id for either (gate 37
 * forbids inventing one), and no endpoint answers a per-account history at all. That is
 * GAP-miniloan-010, open since 2026-09-06 and naming both criteria — this unit does not open a
 * duplicate. What IS measured here is the half these screens can see: a Pending request arrives with
 * both fields null, and an approved one comes back carrying both.
 */
@Component({
  selector: 'app-adjustment-review',
  templateUrl: './adjustment-review.component.html',
  styleUrl: './adjustment-review.component.scss',
})
export class AdjustmentReviewComponent {
  private readonly service = inject(ClosedAccountAdjustmentService);
  private readonly roles = inject(CurrentRoleService);

  readonly id = input.required<string>();

  readonly adjustment = signal<PendingAdjustment | null>(null);
  readonly loading = signal(true);

  /** screens.json unauthorized — the API's own sentence when the READ is refused (ACL-016). */
  readonly notAllowed = signal<string | null>(null);

  /**
   * screens.json error — four-eyes, already decided, or anything else a DECISION comes back with,
   * in the API's own words and rendered beside the request rather than in place of it.
   */
  readonly failed = signal<string | null>(null);

  /** screens.json loading — the pressed button is disabled while its own call is in flight. */
  readonly busy = signal<'' | 'approve' | 'reject'>('');

  /** AC-miniloan-076's sentence or UC-miniloan-018's rejection sentence, as the API worded it. */
  readonly outcome = signal<string | null>(null);

  /**
   * BR-miniloan-049@v1's four eyes, as this screen can see them. The identity compared is the same
   * string the server compares, so the button and the endpoint cannot disagree about who filed it.
   */
  readonly ownRequest = computed(
    () => this.adjustment()?.requestedBy === this.roles.role().id,
  );

  /** ACL-016's condition: STM-miniloan-004 leaves Pending once, and Pending is the only door. */
  readonly decided = computed(() => {
    const status = this.adjustment()?.status;
    return status !== undefined && status !== 'Pending';
  });

  constructor() {
    // input.required() is not readable in the constructor; the house idiom defers one tick.
    queueMicrotask(() => this.load());
  }

  load(): void {
    this.loading.set(true);
    this.notAllowed.set(null);
    this.failed.set(null);

    this.service.view(this.id()).subscribe({
      next: (adjustment) => {
        this.adjustment.set(adjustment);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.adjustment.set(null);
        if (error.status === 403) {
          this.notAllowed.set(this.messageOf(error));
        } else {
          this.failed.set(this.messageOf(error));
        }
        this.loading.set(false);
      },
    });
  }

  approve(): void {
    this.start('approve');
    this.service.approve(this.id()).subscribe({
      next: (decision) => this.settle(decision.adjustment, decision.message),
      error: (error: HttpErrorResponse) => this.refuse(error),
    });
  }

  reject(): void {
    this.start('reject');
    this.service.reject(this.id()).subscribe({
      next: (decision) => this.settle(decision.adjustment, decision.message),
      error: (error: HttpErrorResponse) => this.refuse(error),
    });
  }

  private start(which: 'approve' | 'reject'): void {
    this.busy.set(which);
    this.outcome.set(null);
    this.failed.set(null);
  }

  private settle(adjustment: PendingAdjustment, message: string): void {
    // The decided row the API handed back, not a locally patched copy of the one already on screen.
    this.adjustment.set(adjustment);
    this.outcome.set(message);
    this.busy.set('');
  }

  /**
   * Every refusal of a DECISION lands beside the buttons, the 403s included, and never in the
   * unauthorized branch that replaces the page. AC-miniloan-079 requires the request to still be
   * readable and still Pending after a self-approval is refused — a page that blanked itself would
   * hide the very row the criterion says is unchanged. The unauthorized branch belongs to the READ
   * (ACL-016 · ACL-035), where there is nothing to show in the first place.
   */
  private refuse(error: HttpErrorResponse): void {
    this.failed.set(this.messageOf(error));
    this.busy.set('');
  }

  private messageOf(error: HttpErrorResponse): string {
    const body = error.error as { message?: string } | null;
    return body?.message ?? 'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง';
  }
}

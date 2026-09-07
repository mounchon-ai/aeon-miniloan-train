import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  ClosedAccountAdjustmentService,
  PendingAdjustment,
} from '../../core/services/closed-account-adjustment.service';

/**
 * UI-miniloan-013 · รายการคำขอปรับปรุงบัญชีที่รออนุมัติ (UC-miniloan-018 · ACL-016 · ACL-034 ·
 * ROLE-005).
 *
 * <p><b>The queue is Pending and only Pending, and that is design's condition rather than a filter
 * this page chose.</b> ACL-016 carries `condition: { stateMachine: STM-miniloan-004, state:
 * ["Pending"] }`, and API-024 is declared as `GET /adjustments?status=Pending` — the route refuses
 * any other status outright. So the parameter is sent as design wrote the call, and nothing here
 * narrows what came back.
 *
 * <p><b>ACL-034 is `scope: all`.</b> Unlike UI-miniloan-006 and UI-miniloan-011, whose entries are
 * `scope: own`, the approver sees every pending request in the system; there is no per-person
 * scope to add or to check, and the page renders every row the API returned.
 *
 * <p><b>screens.json's overflow answer is "pagination", and this page pages rows it already
 * holds.</b> API-024 declares no page parameter, so asking the server for a page is not a call this
 * app can make; slicing what arrived is, and it is the only reading of "pagination" that does not
 * silently change what the list contains. UI-miniloan-011 took the same shape for the same reason.
 *
 * <p><b>AC-miniloan-084 and AC-miniloan-085 are not fully provable on this screen, and the card for
 * that is already open.</b> Both measure "เปิดดูประวัติการปรับปรุงของบัญชีนั้น" — a per-account
 * history, with AC-miniloan-085 counting that account's pending requests in its sentence. No
 * endpoint answers either question and no screen in sitemap.json shows `approvedBy` or `approvedAt`.
 * That is GAP-miniloan-010, raised by /dev:plan on 2026-09-06 and still open; this unit does not
 * open a second card for it and does not invent the page.
 */
@Component({
  selector: 'app-pending-adjustments-list',
  imports: [RouterLink],
  templateUrl: './pending-adjustments-list.component.html',
  styleUrl: './pending-adjustments-list.component.scss',
})
export class PendingAdjustmentsListComponent {
  private readonly service = inject(ClosedAccountAdjustmentService);

  /** screens.json overflow — "แบ่งหน้า". A page size no design document names; see the note above. */
  static readonly PAGE_SIZE = 20;

  readonly adjustments = signal<PendingAdjustment[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);

  /** screens.json unauthorized — the API's own sentence, for a caller who is not the approver. */
  readonly notAllowed = signal<string | null>(null);

  /** screens.json error — the load failed, and a retry is offered rather than an empty queue. */
  readonly failed = signal(false);

  readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.adjustments().length / PendingAdjustmentsListComponent.PAGE_SIZE)),
  );

  readonly visible = computed(() => {
    const start = this.page() * PendingAdjustmentsListComponent.PAGE_SIZE;
    return this.adjustments().slice(start, start + PendingAdjustmentsListComponent.PAGE_SIZE);
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.notAllowed.set(null);

    this.service.listPending().subscribe({
      next: (rows) => {
        this.adjustments.set(rows);
        this.page.set(0);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.adjustments.set([]);
        if (error.status === 403) {
          this.notAllowed.set(this.messageOf(error));
        } else {
          this.failed.set(true);
        }
        this.loading.set(false);
      },
    });
  }

  goToPage(page: number): void {
    this.page.set(Math.min(Math.max(page, 0), this.pageCount() - 1));
  }

  private messageOf(error: HttpErrorResponse): string {
    const body = error.error as { message?: string } | null;
    return body?.message ?? 'ไม่มีสิทธิ์เข้าหน้านี้';
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LoanApplication, LoanApplicationService } from '../../core/services/loan-application.service';

/**
 * UI-miniloan-006 · คิวใบสมัครที่มอบหมายให้ฉัน (UC-miniloan-005 · ACL-027).
 *
 * <p><b>The scope is the API's answer.</b> API-005 returns the applications assigned to the calling
 * officer and this page renders exactly what came back — FE-miniloan-023 added that branch to
 * `ScopedListingService` because ACL-027 declares it {@code enforceAt: [api, domain]}, so "own" is a
 * WHERE clause and never a filter applied here. screens.json's unauthorized state says the same
 * thing from the other side: "ไม่เกิด — เห็นเฉพาะใบที่มอบหมายให้ตนเอง", so no branch exists for it.
 *
 * <p><b>The status filter is presentation, not scope.</b> screens.json declares
 * {@code filter-by-status} with {@code kind: filter} and {@code destinationRef: self}, so it narrows
 * rows the API has already decided this officer may see. It never re-asks the server with a filter
 * the caller chose — that would be the browser picking its own scope, which is the hole
 * AC-miniloan-128 describes.
 *
 * <p><b>screens.json's overflow state is not implemented and that is deliberate.</b> It asks for
 * pagination ("จำนวนใบสมัครในคิวเกินหน้าจอ — ใช้การแบ่งหน้า"), but API-005 declares no page
 * parameter and inventing {@code ?page=} would be this unit writing a contract design did not.
 * The whole scoped list is rendered; a card for design is open on it.
 */
@Component({
  selector: 'app-assigned-queue',
  imports: [RouterLink],
  templateUrl: './assigned-queue.component.html',
  styleUrl: './assigned-queue.component.scss',
})
export class AssignedQueueComponent {
  private readonly service = inject(LoanApplicationService);

  readonly applications = signal<readonly LoanApplication[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);

  /** Empty means "ทุกสถานะ" — the filter starts off rather than hiding rows before anyone chose. */
  readonly statusFilter = signal('');

  /** The statuses actually present in the queue, so the control never offers one that matches nothing. */
  readonly statuses = computed(() => [...new Set(this.applications().map((row) => row.status))].sort());

  readonly rows = computed(() => {
    const wanted = this.statusFilter();
    return wanted ? this.applications().filter((row) => row.status === wanted) : this.applications();
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.service.listApplications().subscribe({
      next: (applications) => {
        this.applications.set(applications);
        this.loading.set(false);
      },
      error: () => {
        this.applications.set([]);
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }

  onStatusChange(value: string): void {
    this.statusFilter.set(value);
  }

  /** screens.json's empty state — nothing was assigned, which is not the same as a filter matching none. */
  isEmpty(): boolean {
    return !this.loading() && !this.failed() && this.applications().length === 0;
  }
}

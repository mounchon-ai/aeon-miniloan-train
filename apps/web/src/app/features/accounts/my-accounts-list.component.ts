import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LoanAccountService, LoanAccountSummary } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-011 · บัญชีสินเชื่อที่ฉันดูแล (UC-miniloan-024 · ACL-020 · ACL-032 · ROLE-004).
 *
 * <p><b>The scope is the API's and this page adds none.</b> BR-miniloan-054@v1 is a WHERE clause in
 * {@code LoanAccountScopeService}, and AC-miniloan-137's point is that the hole lives in the LIST
 * rather than in the single read. A browser that filtered again on top would be choosing its own
 * scope, and would look identical to one that had been handed everybody's accounts.
 *
 * <p><b>screens.json's overflow answer is "pagination", and this page paginates rows it already
 * holds.</b> API-013 declares no page parameter, so asking the server for a page is not a call this
 * app can make; slicing what arrived is, and it is the only reading of "pagination" that does not
 * silently change what the list contains. UI-miniloan-006's queue took the same shape for its
 * declared filter, for the same reason.
 *
 * <p><b>{@code next-due-installment} has no entity behind it and that is not a mistake</b> (gate
 * 45): design binds it to nothing, and mock named it in conventions.json fieldMap[]. FE-miniloan-027
 * added it to API-013's row, computed by the API from the current schedule's lowest Due instalment —
 * a fact only the server can see, and one this page therefore never derives.
 */
@Component({
  selector: 'app-my-accounts-list',
  imports: [RouterLink],
  templateUrl: './my-accounts-list.component.html',
  styleUrl: './my-accounts-list.component.scss',
})
export class MyAccountsListComponent {
  private readonly service = inject(LoanAccountService);

  /** screens.json overflow — "แบ่งหน้า". A page size no design document names; see the note above. */
  static readonly PAGE_SIZE = 20;

  readonly accounts = signal<LoanAccountSummary[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly page = signal(0);

  readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.accounts().length / MyAccountsListComponent.PAGE_SIZE)),
  );

  readonly visible = computed(() => {
    const start = this.page() * MyAccountsListComponent.PAGE_SIZE;
    return this.accounts().slice(start, start + MyAccountsListComponent.PAGE_SIZE);
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.service.list().subscribe({
      next: (rows) => {
        this.accounts.set(rows);
        this.page.set(0);
        this.loading.set(false);
      },
      error: (_error: HttpErrorResponse) => {
        this.accounts.set([]);
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }

  goToPage(page: number): void {
    this.page.set(Math.min(Math.max(page, 0), this.pageCount() - 1));
  }

  /** ENT-006's two states in the words AC-miniloan-004 and AC-miniloan-006 use. */
  statusLabel(status: string): string {
    return status === 'Closed' ? 'ปิดบัญชีแล้ว (Closed)' : 'กำลังผ่อนชำระ (Active)';
  }
}

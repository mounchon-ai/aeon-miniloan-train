import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import {
  LoanAccount,
  LoanApplication,
  LoanApplicationService,
} from '../../core/services/loan-application.service';

/**
 * UI-miniloan-002 · รายการใบสมัครและบัญชีของฉัน (UC-miniloan-023).
 *
 * <p><b>The scope is the API's answer, not a filter applied here</b> (AC-miniloan-126 ·
 * AC-miniloan-128 · BR-miniloan-033@v1). This page renders whatever API-005 and API-013 return and
 * never narrows the result: the criterion's warning is that a list is where a scope hole lives, and
 * a page that filtered client-side would have already received the rows it then hid. FE-miniloan-019
 * is where that scope is enforced and measured.
 *
 * <p><b>Four of screens.json's five states are answered here</b> and the fifth is declared not to
 * happen: empty invites a new application, loading shows a skeleton, error offers a retry, and
 * unauthorized "ไม่เกิด — เห็นเฉพาะของตัวเองเสมอ" so no branch exists for it. Two calls go out
 * together and one failure puts the whole page in the error state, because the screen presents the
 * two lists as one answer to "ของฉันมีอะไรบ้าง".
 */
@Component({
  selector: 'app-my-applications-list',
  imports: [RouterLink],
  templateUrl: './my-applications-list.component.html',
  styleUrl: './my-applications-list.component.scss',
})
export class MyApplicationsListComponent {
  private readonly service = inject(LoanApplicationService);

  readonly applications = signal<readonly LoanApplication[]>([]);
  readonly accounts = signal<readonly LoanAccount[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);

    forkJoin({
      applications: this.service.listApplications(),
      accounts: this.service.listAccounts(),
    }).subscribe({
      next: (result) => {
        this.applications.set(result.applications);
        this.accounts.set(result.accounts);
        this.loading.set(false);
      },
      error: () => {
        this.applications.set([]);
        this.accounts.set([]);
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }

  /** screens.json's empty state — nothing at all on either list, not one of the two. */
  isEmpty(): boolean {
    return !this.loading() && !this.failed() && !this.applications().length && !this.accounts().length;
  }
}

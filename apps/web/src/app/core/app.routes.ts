import { Routes } from '@angular/router';

/**
 * One entry per screen this app has built.
 *
 * `/applications` is the path `core/layout/nav.component.ts` committed to for UI-miniloan-002 when
 * FE-miniloan-001 built the shell, so it is honoured here rather than reinvented. The two paths
 * below it are FE-miniloan-021's own: no design document declares URL paths (the shell's build note
 * records that), so `/applications/new` for UI-miniloan-001 and `/applications/:id` for
 * UI-miniloan-003 are picked here, in the same shape, and every later unit that links to either must
 * use these.
 *
 * `new` is declared before `:id` because the router takes the first match and would otherwise read
 * "new" as an application id.
 */
export const routes: Routes = [
  {
    path: 'applications',
    loadComponent: () =>
      import('../features/applications/my-applications-list.component').then(
        (m) => m.MyApplicationsListComponent,
      ),
  },
  {
    path: 'applications/new',
    loadComponent: () =>
      import('../features/applications/application-form.component').then(
        (m) => m.ApplicationFormComponent,
      ),
  },
  {
    path: 'applications/:id',
    loadComponent: () =>
      import('../features/applications/application-detail.component').then(
        (m) => m.ApplicationDetailComponent,
      ),
  },
  // FE-miniloan-022. `/loan-accounts` itself belongs to UI-miniloan-011 (ROLE-004) and is the path
  // nav.component.ts committed to; these two are the applicant's views of one account, reached from
  // UI-miniloan-002's ui-miniloan-002-view-schedule link and from each other.
  {
    path: 'loan-accounts/:id/schedule',
    loadComponent: () =>
      import('../features/applications/repayment-schedule.component').then(
        (m) => m.RepaymentScheduleComponent,
      ),
  },
  {
    path: 'loan-accounts/:id/payoff-quote',
    loadComponent: () =>
      import('../features/applications/early-closure-quote.component').then(
        (m) => m.EarlyClosureQuoteComponent,
      ),
  },
  // FE-miniloan-027. /loan-accounts is the path nav.component.ts committed to for UI-miniloan-011
  // (ROLE-004) when FE-miniloan-001 built the shell, so it is honoured here rather than reinvented.
  // The detail path is this unit's own, in the same shape as its neighbours. It is declared AFTER
  // the two three-segment paths above so a reader sees the specific ones first; Angular matches on
  // whole segment counts, so /loan-accounts/x/schedule could never fall into /loan-accounts/:id.
  {
    path: 'loan-accounts',
    loadComponent: () =>
      import('../features/accounts/my-accounts-list.component').then(
        (m) => m.MyAccountsListComponent,
      ),
  },
  {
    path: 'loan-accounts/:id',
    loadComponent: () =>
      import('../features/accounts/account-detail.component').then((m) => m.AccountDetailComponent),
  },
  // FE-miniloan-023. Neither screen is a sitemap entry node, so nav.component.ts committed to no
  // path for them: the officer reaches the queue from UI-miniloan-009 and the review page from the
  // queue's own ui-miniloan-006-open-application link. These two paths are this unit's contract,
  // in the same shape as the ones above, and later units must link to them rather than invent more.
  {
    path: 'review',
    loadComponent: () =>
      import('../features/review/assigned-queue.component').then((m) => m.AssignedQueueComponent),
  },
  {
    path: 'review/:id',
    loadComponent: () =>
      import('../features/review/application-review.component').then(
        (m) => m.ApplicationReviewComponent,
      ),
  },
  // FE-miniloan-024. Unlike the two above, this path is NOT this unit's to pick: UI-miniloan-010 is
  // a sitemap entry node and nav.component.ts committed to /assignment-queue when FE-miniloan-001
  // built the shell, so it is honoured here rather than reinvented.
  {
    path: 'assignment-queue',
    loadComponent: () =>
      import('../features/assignment/unassigned-queue.component').then(
        (m) => m.UnassignedQueueComponent,
      ),
  },
  // FE-miniloan-025. sitemap.json marks UI-miniloan-008 entry: false, so nav.component.ts links no
  // role to it and this path is this unit's own — picked in the same shape as the others.
  {
    path: 'settings/approver-role',
    loadComponent: () =>
      import('../features/settings/approver-role-setting.component').then(
        (m) => m.ApproverRoleSettingComponent,
      ),
  },
  // FE-miniloan-028. sitemap.json marks BOTH UI-miniloan-013 and UI-miniloan-014 entry: false, so
  // nav.component.ts links no role to either and these two paths are this unit's own -- picked in
  // the same shape as the others. The approver reaches the queue directly and the review page from
  // the queue's own ui-miniloan-013-open-adjustment link (screens.json destinationRef).
  {
    path: 'adjustments',
    loadComponent: () =>
      import('../features/adjustments/pending-adjustments-list.component').then(
        (m) => m.PendingAdjustmentsListComponent,
      ),
  },
  {
    path: 'adjustments/:id',
    loadComponent: () =>
      import('../features/adjustments/adjustment-review.component').then(
        (m) => m.AdjustmentReviewComponent,
      ),
  },
  // FE-miniloan-026. /dashboard is the path nav.component.ts committed to for UI-miniloan-009 when
  // FE-miniloan-001 built the shell (ROLE-002 only), so it is honoured here rather than reinvented.
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
];

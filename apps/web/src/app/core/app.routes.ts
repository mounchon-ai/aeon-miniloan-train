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
];

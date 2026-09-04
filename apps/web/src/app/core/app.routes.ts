import { Routes } from '@angular/router';

/**
 * Deliberately empty for now. Angular's routerLink is a plain string, not
 * checked against this table at compile time, so core/layout/nav.component.ts
 * can already declare its links; each screen's own feature unit (origin:
 * "new", depends_on: FE-miniloan-001) adds its route entry here when it is
 * built, at the path nav.component.ts already committed to.
 */
export const routes: Routes = [];

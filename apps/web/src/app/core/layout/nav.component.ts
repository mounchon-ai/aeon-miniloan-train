import { DOCUMENT } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { CurrentRoleService } from '../services/current-role.service';

/**
 * One entry per sitemap.json node with entry:true, restricted to the single
 * role each is scoped to (design/rbac.json ACL-023/030/031/032). Route paths
 * are this shell's own contract: no design doc declares URL paths, so these
 * are picked here and every feature unit that later implements a screen must
 * register its route at the matching path below rather than inventing a new
 * one — see FE-miniloan-001 build notes.
 *   /applications        UI-miniloan-002  ROLE-001 ผู้สมัคร
 *   /dashboard           UI-miniloan-009  ROLE-002 เจ้าหน้าที่สินเชื่อ
 *   /assignment-queue    UI-miniloan-010  ROLE-003 หัวหน้าเจ้าหน้าที่สินเชื่อ
 *   /loan-accounts       UI-miniloan-011  ROLE-004 ฝ่ายปฏิบัติการ
 */
interface NavEntry {
  screen: string;
  label: string;
  path: string;
  role: string;
}

const NAV_ENTRIES: readonly NavEntry[] = [
  { screen: 'UI-miniloan-002', label: 'ใบสมัครและบัญชีของฉัน', path: '/applications', role: 'ROLE-001' },
  { screen: 'UI-miniloan-009', label: 'แดชบอร์ด', path: '/dashboard', role: 'ROLE-002' },
  { screen: 'UI-miniloan-010', label: 'คิวมอบหมายใบสมัคร', path: '/assignment-queue', role: 'ROLE-003' },
  { screen: 'UI-miniloan-011', label: 'บัญชีสินเชื่อที่ฉันดูแล', path: '/loan-accounts', role: 'ROLE-004' },
];

@Component({
  selector: 'app-nav',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './nav.component.html',
  styleUrl: './nav.component.scss',
})
export class NavComponent {
  private readonly currentRole = inject(CurrentRoleService);
  private readonly document = inject(DOCUMENT);

  /**
   * The nav link's data-testid, keyed by the DESTINATION screen — mock's own
   * navControlId (mock/scripts/wireframes.mjs, gate 69), copied rather than
   * invented here. These ids are shell furniture and deliberately sit outside
   * every wireframe's controls[] (mock/scripts/html.mjs mintedTestids), which
   * is exactly why dev may render them but cannot record them in
   * manifest.ui_controls — see GAP-miniloan-002's answer and ADR-004.
   */
  navTestId(screen: string): string {
    return `nav-${screen.toLowerCase()}`;
  }

  readonly roles = this.currentRole.roles;
  readonly role = this.currentRole.role;

  readonly entries = computed(() =>
    NAV_ENTRIES.filter((entry) => entry.role === this.currentRole.role().id),
  );

  /**
   * Switching role changes WHO is asking, so everything already on screen belongs to somebody
   * else: each feature component loads once in its constructor and listens to no role signal, so
   * without this the previous role's rows stay rendered while the API would answer differently
   * (a Loan Officer kept seeing the applicant's own list, which GET /applications answers with []
   * for that token). A reload is the honest scope of the change rather than a re-fetch bolted onto
   * eight components: the token, every list and every unsent form belonged to the role that just
   * left. CurrentRoleService writes the choice to sessionStorage first, so the reload comes back
   * as the role that was picked and not as ROLE-001.
   */
  onRoleChange(roleId: string): void {
    if (this.currentRole.setRole(roleId)) {
      this.document.defaultView?.location.reload();
    }
  }
}

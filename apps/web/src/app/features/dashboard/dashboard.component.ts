import { Component, inject, signal } from '@angular/core';

import { DashboardService, DashboardSummary } from '../../core/services/dashboard.service';

/**
 * UI-miniloan-009 · แดชบอร์ดภาพรวมสถานะ (UC-miniloan-020 · ACL-030).
 *
 * <p><b>Five numbers, all of them the API's.</b> AC-miniloan-110 wants "ยื่นแล้ว 3 · อยู่ระหว่าง
 * พิจารณา 2 · อนุมัติแล้ว 1 · ใช้งานอยู่ 5 · ปิดแล้ว 4" — the first three counted from applications
 * and the last two from loan accounts, one set of numbers out of two aggregates. Nothing on this page
 * adds, totals or derives: AC-miniloan-112 is the case that shows why, because a Disbursed
 * application already has an Active account and must be counted once as Active and never again under
 * Approved. That decision lives in {@code DashboardSummaryService} and a browser that repeated it
 * would be the second place it could be got wrong.
 *
 * <p><b>Zero is a number, not an absence</b> (AC-miniloan-111). "เป็นเลขศูนย์ ไม่ใช่ช่องว่าง ไม่ใช่ขีด
 * และไม่ใช่ข้อความว่าไม่มีข้อมูล" — so the empty state renders the same five fields with 0 in them
 * and there is deliberately no "ยังไม่มีข้อมูล" branch, unlike every list screen in this app. A
 * {@code ?? '—'} anywhere here would break the criterion while looking like the house style.
 *
 * <p><b>screens.json declares no unauthorized branch</b> — "ไม่เกิด — หน้านี้เปิดให้เฉพาะ Loan Officer
 * ที่ล็อกอินอยู่แล้วเสมอ", and FE-miniloan-001's nav links /dashboard to ROLE-002 alone. The API still
 * refuses anyone else (DASHBOARD_NOT_PERMITTED), and that refusal lands in the error state here
 * rather than in a branch design says cannot happen.
 *
 * <p><b>The scope these numbers cover is settled now.</b> AC-miniloan-112 closes with "ขอบเขตข้อมูล
 * ที่ Loan Officer แต่ละคนเห็นยังไม่ตัดสิน (DQ-miniloan-004)"; ADR-007 decided it on 2026-09-07 —
 * ACL-018 and ACL-030 are scope: all, so these are the system's totals and not one officer's.
 */
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly service = inject(DashboardService);

  readonly summary = signal<DashboardSummary | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  constructor() {
    this.load();
  }

  /** Also the refresh action (screens.json: "ดึงตัวเลขล่าสุดมาแสดงใหม่", destinationRef self). */
  load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.service.summary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: () => {
        this.summary.set(null);
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }
}

import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  ApplicationDetail,
  LoanApplicationService,
} from '../../core/services/loan-application.service';

/**
 * UI-miniloan-003 · รายละเอียดใบสมัครของฉัน (UC-miniloan-023 · UC-miniloan-002).
 *
 * <p><b>All five of screens.json's states are answered, including the one the other two screens
 * declare impossible.</b> "unauthorized: แสดงข้อความว่าไม่มีสิทธิ์ดูใบสมัครนี้ เมื่อพยายามเปิด
 * ใบสมัครของผู้อื่นตาม BR-miniloan-033@v1" is a real branch here, and it renders the API's own
 * sentence — FE-miniloan-019 made API-004 answer 403 "ไม่มีสิทธิ์เข้าถึงใบสมัครนี้" for exactly this
 * case, so the page shows what came back rather than deciding anything. AC-miniloan-127's whole
 * point is that the refusal is the API's; hiding the link would not have been enough.
 *
 * <p><b>The assessment block is what the API returned or nothing.</b> ENT-003's band, ceiling and
 * reasons are absent on a draft (AC-miniloan-035) and the API says so in {@code assessmentNote};
 * this page never computes a band and never re-words a reason line.
 */
@Component({
  selector: 'app-application-detail',
  imports: [RouterLink],
  templateUrl: './application-detail.component.html',
  styleUrl: './application-detail.component.scss',
})
export class ApplicationDetailComponent {
  private readonly service = inject(LoanApplicationService);

  /** Bound from the route's :id — provideRouter's withComponentInputBinding is on in app.config. */
  readonly id = input.required<string>();

  readonly detail = signal<ApplicationDetail | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  /** BR-miniloan-033@v1's refusal, in the API's own words (AC-miniloan-127). */
  readonly notAllowed = signal<string | null>(null);

  constructor() {
    queueMicrotask(() => this.load());
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.notAllowed.set(null);

    this.service.detail(this.id()).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 403) {
          this.notAllowed.set(error.error?.message ?? null);
        } else {
          this.failed.set(true);
        }
        this.detail.set(null);
        this.loading.set(false);
      },
    });
  }
}

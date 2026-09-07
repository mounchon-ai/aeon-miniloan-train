import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { RepaymentSchedule, LoanAccountService } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-004 · ตารางผ่อนชำระของฉัน (UC-miniloan-011 · API-012 · BR-miniloan-018@v1 ·
 * BR-miniloan-033@v1 · ACL-010).
 *
 * <p><b>Every row the API sent, in the order it sent them</b> (AC-miniloan-098). "ทุกงวดตั้งแต่งวดที่ 1
 * ถึงงวดสุดท้าย ไม่ใช่เฉพาะงวดที่ยังไม่ถึงกำหนด" is a rule about what is shown, and the way to break
 * it from here would be a filter on status or due date. There is none: the template renders
 * {@code installments} whole. {@link RepaymentScheduleQueryService} already refuses to drop a paid
 * row on the API side, and this screen must not undo that.
 *
 * <p><b>The refusal is the API's, in the API's own words</b> (AC-miniloan-099). "ก. เรียกดูตารางผ่อน
 * ของบัญชี ข. … การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ไม่แสดงลิงก์บนหน้าจอ" — so a 403 renders the
 * message that came back, and no instalment of anyone else's is on the page, because the API sent
 * none. This screen's sentence is its own: {@code RepaymentScheduleController} answers
 * "ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้", which is not the sentence UI-miniloan-005 gets.
 *
 * <p><b>The summary figure is not a row.</b> "เงินต้นรวมทั้งตาราง" is ENT-007's {@code totalPrincipal}
 * and screens.json puts it in the schedule-summary zone beside the payoff action, not inside the
 * table — nothing here adds up the instalments to reach it.
 */
@Component({
  selector: 'app-repayment-schedule',
  imports: [RouterLink],
  templateUrl: './repayment-schedule.component.html',
  styleUrl: './repayment-schedule.component.scss',
})
export class RepaymentScheduleComponent {
  private readonly service = inject(LoanAccountService);

  /** Bound from the route's :id (withComponentInputBinding, turned on in FE-miniloan-021). */
  readonly id = input.required<string>();

  readonly schedule = signal<RepaymentSchedule | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  /** screens.json unauthorized · AC-miniloan-099 — BR-miniloan-033@v1's refusal, as it arrived. */
  readonly notAllowed = signal<string | null>(null);

  constructor() {
    queueMicrotask(() => this.load());
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.notAllowed.set(null);

    this.service.schedule(this.id()).subscribe({
      next: (schedule) => {
        this.schedule.set(schedule);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 403) {
          this.notAllowed.set(error.error?.message ?? null);
        } else {
          this.failed.set(true);
        }
        this.schedule.set(null);
        this.loading.set(false);
      },
    });
  }
}

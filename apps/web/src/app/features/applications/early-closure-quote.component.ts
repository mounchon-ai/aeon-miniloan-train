import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LoanAccountService, PayoffQuote } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-005 · ยอดปิดบัญชีก่อนกำหนดของฉัน (UC-miniloan-015 · API-015 · BR-miniloan-022@v1 ·
 * CALC-miniloan-004@v1 · ACL-013).
 *
 * <p><b>Four figures and a date, none of them worked out here.</b> BR-miniloan-022@v1's numbers are
 * CALC-miniloan-004@v1's and are signed as GD-miniloan-005; the page asks API-015 and renders what
 * comes back. AC-miniloan-073 and AC-miniloan-075 quote the day count as part of what the borrower
 * reads — "ดอกเบี้ยค้างจ่าย 10 วัน" — so {@code daysElapsed} is rendered beside the amount rather
 * than treated as an internal step.
 *
 * <p><b>The date shown is the server's, not the one this page asked with.</b> API-015 takes a
 * closing date, so a caller must supply one and today's date is the request's own timestamp — not a
 * business decision. What "วันที่คำนวณยอด" displays is {@link PayoffQuote.closingDate}, the value
 * the API echoed back, which is the same reasoning that put {@code submittedAt} on the application
 * DTO in FE-miniloan-021: a screen shows what the server decided, never what the browser guessed.
 *
 * <p><b>There is no close-account button, and that is deliberate.</b> AC-miniloan-072 describes an
 * applicant pressing "ยืนยันปิดบัญชี" and being refused, but screens.json declares exactly one
 * action on this screen — back-to-schedule — and mock minted exactly one action id. A button dev
 * invented would be a control nobody asked for (DV17 Class B) carrying a testid nobody minted
 * (gate 37). The API half of that criterion is built and measured: the settlement route is
 * Operations-only. What this page can honestly do is offer no such action at all, and its spec
 * asserts that. The user-visible sentence AC-miniloan-072 quotes has no field in screens.json to
 * hold it — a card for design, raised through /dev:plan.
 *
 * <p>The refusal sentence here is this screen's own: {@code EarlyClosureQuoteService} answers
 * "ไม่มีสิทธิ์ขอยอดปิดบัญชีก่อนกำหนด — ทำได้เฉพาะเจ้าของบัญชี", which is not the sentence
 * UI-miniloan-004 gets for the same rule.
 */
@Component({
  selector: 'app-early-closure-quote',
  imports: [RouterLink],
  templateUrl: './early-closure-quote.component.html',
  styleUrl: './early-closure-quote.component.scss',
})
export class EarlyClosureQuoteComponent {
  private readonly service = inject(LoanAccountService);

  /** Bound from the route's :id (withComponentInputBinding, turned on in FE-miniloan-021). */
  readonly id = input.required<string>();

  readonly quote = signal<PayoffQuote | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  /** screens.json unauthorized — BR-miniloan-033@v1's refusal for this screen, as it arrived. */
  readonly notAllowed = signal<string | null>(null);

  constructor() {
    queueMicrotask(() => this.load());
  }

  /** screens.json: "คำนวณยอดใหม่ทุกครั้งที่เข้าหน้า" — one call per visit, and per retry. */
  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.notAllowed.set(null);

    this.service.payoffQuote(this.id(), EarlyClosureQuoteComponent.today()).subscribe({
      next: (quote) => {
        this.quote.set(quote);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 403) {
          this.notAllowed.set(error.error?.message ?? null);
        } else {
          this.failed.set(true);
        }
        this.quote.set(null);
        this.loading.set(false);
      },
    });
  }

  /** AC-miniloan-071's "ณ วันที่ขอ", as the ISO date API-015 expects. */
  private static today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}

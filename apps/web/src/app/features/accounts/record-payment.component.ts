import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { LoanAccountService, PaymentResult } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-012's `payment-form` zone (UC-miniloan-012 · UC-miniloan-013 · UC-miniloan-016 ·
 * ACL-011 · ACL-012 · ACL-014).
 *
 * <p><b>One capture field and two actions</b>, exactly as screens.json declares them:
 * `payment-amount-input` bound to ENT-009.amount, then `record-payment` and
 * `record-payoff-payment`. Neither number this component adds to the request is a choice it made:
 *
 * <ul>
 *   <li><b>installmentNumber</b> — ACL-011 and ACL-012 both carry
 *       `condition: { stateMachine: STM-miniloan-003, state: ["Due"] }`, which is design saying the
 *       payment targets a Due instalment; the page is already holding the schedule the API sent, so
 *       it sends the earliest Due row's number and lets the API refuse if it disagrees. When nothing
 *       is owing it sends 0, and the attempt still goes out: AC-miniloan-088 requires Operations to
 *       press this on a Closed account and be refused BY THE API, so a page that declined to call
 *       would break the criterion's screen half. ENT-008 numbers instalments from 1, so 0 names no
 *       row and can only be refused — it never mis-targets a live one. This is why the field is not
 *       the absence FE-miniloan-024's officer picker was: there, an empty string was a value the
 *       API could act on wrongly.
 *   <li><b>closingDate</b> — today, exactly as FE-miniloan-022's quote page sends it. The endpoint
 *       takes a date; the day the request is made is not a business decision.
 * </ul>
 *
 * <p><b>The outcome sentence is composed HERE, and the API's own note says it must be.</b>
 * PaymentResponse's Javadoc records that AC-miniloan-013's and AC-miniloan-086's sentences belong to
 * the screen and that what travels is every number they are built from. Refusals are the other way
 * round: those are the API's behaviour and its wording is rendered untouched (AC-miniloan-012 ·
 * AC-miniloan-014 · AC-miniloan-070 · AC-miniloan-088).
 *
 * <p><b>AC-miniloan-070's screen half is already true.</b> "หน้าจอไม่มีปุ่ม บันทึกการชำระ ให้กด" —
 * UI-miniloan-012 declares roles: [ROLE-004] and nav links it to nobody else, so an Applicant never
 * reaches this component. Re-gating the buttons inside it would be a second answer to a question the
 * sitemap already settled, and the criterion's other half — that the refusal happens at the API — is
 * measured in PaymentControllerTest, where BR-miniloan-025@v1 lives.
 */
@Component({
  selector: 'app-record-payment',
  imports: [ReactiveFormsModule],
  templateUrl: './record-payment.component.html',
  styleUrl: './record-payment.component.scss',
})
export class RecordPaymentComponent {
  private readonly service = inject(LoanAccountService);

  readonly accountId = input.required<string>();

  /** The earliest Due instalment in the schedule the parent loaded, or null when nothing is owing. */
  readonly nextDueInstallment = input<number | null>(null);

  /** The parent re-reads the account and the schedule; this component never mutates either. */
  readonly recorded = output<void>();

  readonly amount = new FormControl('', { nonNullable: true });

  readonly busy = signal<'' | 'payment' | 'payoff'>('');

  /** AC-miniloan-013 · AC-miniloan-086 · AC-miniloan-087 — built here from the API's figures. */
  readonly outcome = signal<string | null>(null);

  /** Every refusal, in the API's own words. */
  readonly failed = signal<string | null>(null);

  recordPayment(): void {
    this.start('payment');
    this.service
      .recordPayment(this.accountId(), this.nextDueInstallment() ?? 0, this.amount.value)
      .subscribe({
        next: (result) => {
          this.outcome.set(this.sentenceFor(result));
          this.finish();
        },
        error: (error: HttpErrorResponse) => this.refuse(error),
      });
  }

  recordPayoffPayment(): void {
    this.start('payoff');
    this.service.recordPayoffPayment(this.accountId(), this.today(), this.amount.value).subscribe({
      next: () => {
        // AC-miniloan-005's sentence is about the ACCOUNT after it closed, so the parent's summary
        // renders it from closeReason rather than this component composing a second copy.
        this.outcome.set(null);
        this.finish();
      },
      error: (error: HttpErrorResponse) => this.refuse(error),
    });
  }

  /**
   * AC-miniloan-013: "บันทึกการชำระงวดที่ 3 เรียบร้อย".
   * AC-miniloan-086 · AC-miniloan-087: the same opening, then the three figures the API returned.
   *
   * <p>The branch is the API's `paymentType`, never a comparison this page makes: a browser that
   * decided "this was an overpayment" would be answering BR-miniloan-019@v1 for itself.
   */
  private sentenceFor(result: PaymentResult): string {
    const opening = `บันทึกการชำระงวดที่ ${result.installmentNumber} เรียบร้อย`;
    if (result.paymentType !== 'InstallmentOverpayment') {
      return opening;
    }
    return (
      `${opening} — ส่วนเกิน ${this.money(result.overpaymentAmount)} บาท` +
      ` · ค่าธรรมเนียมการโปะ ${this.money(result.prepaymentFee)} บาท` +
      ` · ตัดเงินต้น ${this.money(result.principalReduction)} บาท`
    );
  }

  /**
   * Presentation only. The figure is the API's — BR-miniloan-035@v1 already rounded it half-up at
   * the two places DQ-miniloan-001 settled — and this puts the group separators and the two decimal
   * places the criteria quote back on a JSON number that cannot carry a trailing zero. It rounds
   * nothing that was not already rounded, and it never chooses between two figures.
   */
  private money(value: string | number | null): string {
    return Number(value ?? 0).toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private start(which: 'payment' | 'payoff'): void {
    this.busy.set(which);
    this.outcome.set(null);
    this.failed.set(null);
  }

  private finish(): void {
    this.busy.set('');
    this.amount.setValue('');
    this.recorded.emit();
  }

  private refuse(error: HttpErrorResponse): void {
    const body = error.error as { message?: string } | null;
    // screens.json: the amount is deliberately left in the control so a retry does not retype it.
    this.failed.set(body?.message ?? 'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง');
    this.busy.set('');
  }
}

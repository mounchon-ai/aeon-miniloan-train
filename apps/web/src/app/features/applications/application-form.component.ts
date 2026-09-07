import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  DraftFields,
  LoanApplicationService,
} from '../../core/services/loan-application.service';

/**
 * UI-miniloan-001 · กรอกและยื่นใบสมัครสินเชื่อ (UC-miniloan-001 · UC-miniloan-002 · UC-miniloan-028).
 *
 * <p><b>No rule is checked here.</b> BR-miniloan-027@v1 and REQ-miniloan-006 make this a thin
 * client, so the form has no range validator, no required-field validator and no arithmetic:
 * AC-miniloan-026's two range messages and AC-miniloan-030's list of missing fields are both read
 * off the API's own refusal and rendered as they arrived. Adding an Angular `Validators.min(10000)`
 * would be a second copy of BR-miniloan-004@v1 that could disagree with the first, and it would let
 * a form pass locally that the API refuses.
 *
 * <p><b>Save-draft and submit ask different things of the same data</b> (AC-miniloan-032 ·
 * AC-miniloan-033). A draft is saved exactly as typed — 5,000 บาท and four empty boxes included —
 * and the API says so; only submit is refused. That difference is entirely the API's, and this
 * component simply calls the endpoint the button belongs to.
 *
 * <p><b>The ceiling comes from the API on every change</b> (AC-miniloan-117). The income box asks
 * API-021 each time it changes and renders whatever comes back; when the call fails the field shows
 * BR-miniloan-028@v1's sentence instead of a number (AC-miniloan-118), because a guessed figure
 * could disagree with the one used at approval.
 *
 * <p><b>Submitted is read-only and the second click is refused</b> (AC-miniloan-031 ·
 * AC-miniloan-133). Once the API reports Submitted the inputs are disabled and the page says so;
 * the buttons also disable while a call is in flight, but that is a courtesy, not the guard —
 * AC-miniloan-134 puts the real one on a database constraint, and when the API refuses a repeat this
 * component shows that refusal rather than swallowing it.
 */
@Component({
  selector: 'app-application-form',
  imports: [ReactiveFormsModule],
  templateUrl: './application-form.component.html',
  styleUrl: './application-form.component.scss',
})
export class ApplicationFormComponent {
  private readonly service = inject(LoanApplicationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  /** BR-miniloan-028@v1's sentence for a preview that could not be fetched (AC-miniloan-118). */
  static readonly PREVIEW_UNAVAILABLE = 'ตอนนี้ดึงข้อมูลวงเงินไม่ได้ กรุณาลองใหม่อีกครั้ง';

  readonly form = this.fb.group({
    fullName: [''],
    age: [null as number | null],
    monthlyIncome: [''],
    currentEmploymentMonths: [null as number | null],
    existingMonthlyDebt: [''],
    requestedAmount: [''],
    requestedTermMonths: [null as number | null],
  });

  readonly applicationId = signal<string | null>(null);
  readonly status = signal<string | null>(null);
  readonly saving = signal(false);
  readonly submitting = signal(false);

  /** Whatever the API said — a success note, a refusal, or a list of missing fields. */
  readonly message = signal<string | null>(null);
  readonly fieldErrors = signal<readonly string[]>([]);

  /** The ceiling as API-021 returned it, or the sentence that says it could not be fetched. */
  readonly maxApprovablePreview = signal<string | null>(null);
  readonly previewError = signal<string | null>(null);

  /** AC-miniloan-031 — once the API reports Submitted, nothing on this form may be edited again. */
  readonly locked = signal(false);

  onIncomeChange(value: string): void {
    if (!value) {
      this.maxApprovablePreview.set(null);
      this.previewError.set(null);
      return;
    }
    this.service.previewMaxApprovable(value).subscribe({
      next: (result) => {
        this.maxApprovablePreview.set(result.maxApprovableAmount);
        this.previewError.set(null);
      },
      error: () => {
        this.maxApprovablePreview.set(null);
        this.previewError.set(ApplicationFormComponent.PREVIEW_UNAVAILABLE);
      },
    });
  }

  onSaveDraft(): void {
    if (this.locked()) {
      return;
    }
    this.saving.set(true);
    this.clearMessages();

    const existing = this.applicationId();
    const fields = this.fields();
    const call = existing ? this.service.updateDraft(existing, fields) : this.service.saveDraft(fields);

    call.subscribe({
      next: (application) => {
        this.applicationId.set(application.id);
        this.status.set(application.status);
        this.message.set('บันทึกร่างเรียบร้อย');
        this.saving.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.showApiRefusal(error);
        this.saving.set(false);
      },
    });
  }

  onSubmit(): void {
    const id = this.applicationId();
    if (!id || this.locked()) {
      return;
    }
    this.submitting.set(true);
    this.clearMessages();

    this.service.submit(id).subscribe({
      next: (detail) => {
        this.status.set(detail.application.status);
        this.locked.set(true);
        this.form.disable();
        this.submitting.set(false);
        this.router.navigate(['/applications', id]);
      },
      error: (error: HttpErrorResponse) => {
        this.showApiRefusal(error);
        this.submitting.set(false);
      },
    });
  }

  /**
   * The API's own words, never a sentence assembled here. `missingFields` rides along on
   * AC-miniloan-030's refusal so every empty box is named at once rather than the first one only.
   */
  private showApiRefusal(error: HttpErrorResponse): void {
    const body = error.error ?? {};
    this.message.set(body.message ?? null);
    this.fieldErrors.set(Array.isArray(body.missingFields) ? body.missingFields : []);
  }

  private clearMessages(): void {
    this.message.set(null);
    this.fieldErrors.set([]);
  }

  private fields(): DraftFields {
    const value = this.form.getRawValue();
    return {
      fullName: value.fullName || null,
      age: value.age ?? null,
      monthlyIncome: value.monthlyIncome || null,
      currentEmploymentMonths: value.currentEmploymentMonths ?? null,
      existingMonthlyDebt: value.existingMonthlyDebt || null,
      requestedAmount: value.requestedAmount || null,
      requestedTermMonths: value.requestedTermMonths ?? null,
    };
  }
}

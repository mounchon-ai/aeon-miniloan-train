import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ADJUSTABLE_FIELDS, LoanAccountService } from '../../core/services/loan-account.service';

/**
 * UI-miniloan-012's `adjustment-form` zone (UC-miniloan-017 · ACL-015 · ROLE-004).
 *
 * <p><b>Four capture fields, one action</b> — screens.json's list, and all four are ENT-010's
 * required capture attributes: targetRecordId, fieldName, oldValue, newValue.
 *
 * <p><b>Every sentence after a submit is the API's.</b> AC-miniloan-076 words it "ส่งคำขอปรับปรุง
 * บัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก {role ผู้อนุมัติ}", and the approver role is a row only
 * the server can read (BR-miniloan-039@v1). AC-miniloan-080's refusal — "ยังไม่ได้ตั้ง role
 * ผู้อนุมัติ …" — comes back the same way. A page that composed either would be guessing at a
 * setting it has not been told, and AC-miniloan-081 exists precisely because that guess is the
 * tempting one.
 *
 * <p><b>The form is always sent, empty values included.</b> AC-miniloan-080 and AC-miniloan-081 both
 * require the refusal to happen at the API rather than by this page declining to call, and
 * BR-miniloan-025@v1 says so in general.
 *
 * <p><b>AC-miniloan-077's and AC-miniloan-078's screen halves are structural, not gated here.</b>
 * "ช่องข้อมูลบนหน้าบัญชีที่ปิดแล้วอยู่ในสถานะแก้ไม่ได้" is true because the parent renders the
 * account summary as text and mints no editable control over it; "ยกเลิกหรือลบบัญชี" has no action
 * on this screen at all, so there is no button to press. Both refusals are also measured at the API,
 * where BR-miniloan-025@v1 puts them.
 */
@Component({
  selector: 'app-submit-adjustment',
  imports: [ReactiveFormsModule],
  templateUrl: './submit-adjustment.component.html',
  styleUrl: './submit-adjustment.component.scss',
})
export class SubmitAdjustmentComponent {
  private readonly service = inject(LoanAccountService);

  readonly accountId = input.required<string>();

  readonly submitted = output<void>();

  /** ENT-010's declared enum, as design spells it. Nothing here names them in Thai. */
  readonly fields = ADJUSTABLE_FIELDS;

  readonly form = new FormGroup({
    targetRecordId: new FormControl('', { nonNullable: true }),
    fieldName: new FormControl('', { nonNullable: true }),
    oldValue: new FormControl('', { nonNullable: true }),
    newValue: new FormControl('', { nonNullable: true }),
  });

  readonly busy = signal(false);

  /** AC-miniloan-076's sentence, exactly as the API worded it. */
  readonly outcome = signal<string | null>(null);

  /** AC-miniloan-080 · AC-miniloan-107's family of refusals, likewise untouched. */
  readonly failed = signal<string | null>(null);

  submit(): void {
    this.busy.set(true);
    this.outcome.set(null);
    this.failed.set(null);

    this.service.submitAdjustment(this.accountId(), this.form.getRawValue()).subscribe({
      next: (result) => {
        this.outcome.set(result.message);
        this.form.reset();
        this.busy.set(false);
        this.submitted.emit();
      },
      error: (error: HttpErrorResponse) => {
        const body = error.error as { message?: string } | null;
        // screens.json: what was typed stays, so a retry does not make somebody fill it again.
        this.failed.set(body?.message ?? 'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง');
        this.busy.set(false);
      },
    });
  }
}

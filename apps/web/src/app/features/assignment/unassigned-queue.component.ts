import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';

import { LoanApplication, LoanApplicationService } from '../../core/services/loan-application.service';

/** The two commands screens.json declares in the unassigned-queue zone. */
type Command = 'assign' | 'cancel';

/**
 * UI-miniloan-010 · คิวใบสมัครที่ยังไม่ถูกมอบหมาย (UC-miniloan-004 · UC-miniloan-008 · ACL-031).
 *
 * <p><b>"Unassigned" is this screen's subject, not a scope.</b> ACL-031 gives ROLE-003
 * {@code scope: all} and API-005 returns every application accordingly, so the rows without an
 * {@code assignedLoanOfficerId} are picked out here — narrowing a list the API already decided this
 * caller may see, the same shape as UI-miniloan-006's status filter. FE-miniloan-024 added that field
 * to the API because ENT-002 declares it and the page had no other way to tell the two apart.
 *
 * <p><b>Neither command can succeed from this screen, and that is reported rather than papered
 * over.</b> screens.json declares two actions here and <b>zero</b> capture fields:
 *
 * <ul>
 *   <li>{@code assign-officer} needs a Loan Officer to hand the application to. No field on this
 *       screen captures one, mock minted no id for such a control, and — worse than
 *       AC-miniloan-054's missing amount input — <b>no endpoint anywhere returns a list of Loan
 *       Officers to choose from</b>. ENT-004 Staff exists; nothing exposes it. A picker invented here
 *       would be a control nobody asked for (DV17 Class B) carrying a testid nobody minted (gate 37),
 *       populated from a source that does not exist.
 *   <li>{@code cancel-application} needs a reason: BR-miniloan-047@v1 requires one and
 *       AC-miniloan-090 says "พร้อมระบุเหตุผล". UI-miniloan-007 has {@code cancel-reason-input};
 *       UI-miniloan-010 has nothing.
 * </ul>
 *
 * <p>So both buttons are rendered — they are declared actions with minted ids — and both send what
 * the screen actually has. The API refuses with {@code LOAN_OFFICER_REQUIRED} and
 * {@code CANCELLATION_REASON_REQUIRED}, which is BR-miniloan-032@v1 and BR-miniloan-047@v1 being
 * enforced where AC-miniloan-091 says they must be. Two cards are open for design.
 *
 * <p><b>The cancel button is not gated on role, deliberately.</b> AC-miniloan-091 has a non-supervisor
 * press it "ทั้งจากหน้าจอ และด้วยการเรียก API ตรง" and be refused both ways — "การปฏิเสธเกิดที่ฝั่ง API
 * ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ". A page that hid the button would make the criterion unmeasurable.
 *
 * <p><b>screens.json's overflow state is not implemented,</b> for the same reason as UI-miniloan-006:
 * it asks for pagination and API-005 declares no page parameter. A card is open on it.
 */
@Component({
  selector: 'app-unassigned-queue',
  templateUrl: './unassigned-queue.component.html',
  styleUrl: './unassigned-queue.component.scss',
})
export class UnassignedQueueComponent {
  private readonly service = inject(LoanApplicationService);

  readonly applications = signal<readonly LoanApplication[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);

  /** screens.json unauthorized — the API's own sentence, never one written here. */
  readonly notAllowed = signal<string | null>(null);

  /** The API's refusal or confirmation for the last command pressed, and which row it was. */
  readonly commandError = signal<string | null>(null);
  readonly outcome = signal<string | null>(null);
  readonly busy = signal<{ command: Command; id: string } | null>(null);

  /** The queue itself: the applications nobody holds yet. */
  readonly rows = computed(() =>
    this.applications().filter((application) => application.assignedLoanOfficerId === null),
  );

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.notAllowed.set(null);

    this.service.listApplications().subscribe({
      next: (applications) => {
        this.applications.set(applications);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.applications.set([]);
        if (error.status === 403) {
          this.notAllowed.set(this.messageOf(error));
        } else {
          this.failed.set(true);
        }
        this.loading.set(false);
      },
    });
  }

  /**
   * API-006. The officer id is empty because this screen has no field that holds one — see the class
   * note. The call still goes out: BR-miniloan-032@v1 is the API's rule and its refusal is the proof
   * the rule is enforced there.
   */
  assign(id: string): void {
    this.busy.set({ command: 'assign', id });
    this.commandError.set(null);
    this.outcome.set(null);

    this.service.assign(id, '').subscribe({
      next: (result) => {
        this.outcome.set(
          `มอบหมายเรียบร้อย — ผู้รับผิดชอบ: ${result.assignedLoanOfficerId} ` +
            `(มอบหมายโดย ${result.assignedBy} เมื่อ ${result.assignedAt})`,
        );
        this.busy.set(null);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.commandError.set(this.messageOf(error));
        this.busy.set(null);
      },
    });
  }

  /** API-009. Likewise: no reason field on this screen, so the API's rule is what answers. */
  cancel(id: string): void {
    this.busy.set({ command: 'cancel', id });
    this.commandError.set(null);
    this.outcome.set(null);

    this.service.cancel(id, '').subscribe({
      next: (result) => {
        this.outcome.set(
          'ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง · ' +
            `ยกเลิกโดย ${result.cancelledBy} เมื่อ ${result.cancelledAt}`,
        );
        this.busy.set(null);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.commandError.set(this.messageOf(error));
        this.busy.set(null);
      },
    });
  }

  isBusy(command: Command, id: string): boolean {
    const busy = this.busy();
    return busy !== null && busy.command === command && busy.id === id;
  }

  /** screens.json empty — nothing is waiting to be handed out, which is not a failure. */
  isEmpty(): boolean {
    return !this.loading() && !this.failed() && !this.notAllowed() && this.rows().length === 0;
  }

  /**
   * AC-miniloan-131's sentence is the only message this app invents, and it exists because a timeout
   * has no response body to quote.
   */
  private messageOf(error: HttpErrorResponse): string {
    const body = error.error as { message?: string } | null;
    return body?.message ?? 'ดำเนินการไม่สำเร็จ — ระบบไม่ตอบสนอง กรุณาสั่งใหม่อีกครั้ง';
  }
}

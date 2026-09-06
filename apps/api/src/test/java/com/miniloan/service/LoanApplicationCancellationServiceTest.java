package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ยกเลิกใบสมัคร (UC-miniloan-007 · UC-miniloan-008 · BR-miniloan-047@v1 · BR-miniloan-031@v2).
 *
 * <p>The rule under test is the one AC-miniloan-092 states outright: the right to cancel MOVES at
 * the moment of assignment, and is never held by two people at once. Every case below is the same
 * application at a different point on that line.
 */
@SpringBootTest
class LoanApplicationCancellationServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String ANOTHER_OFFICER = "STAFF-ข";
    private static final String REASON = "ผู้สมัครแจ้งขอถอนเรื่อง";

    @Autowired private LoanApplicationCancellationService cancellationService;
    @Autowired private LoanApplicationApprovalService approvalService;
    @Autowired private LoanApplicationRejectionService rejectionService;
    @Autowired private ApplicationAssignmentService assignmentService;
    @Autowired private LoanApplicationSubmitService submitService;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private LoanApplicationRepository applications;
    @Autowired private ApplicationAssignmentRepository assignments;
    @Autowired private CreditAssessmentRepository assessments;
    @Autowired private IdempotencyKeyRepository idempotencyKeys;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean() {
        assignments.deleteAll();
        assessments.deleteAll();
        idempotencyKeys.deleteAll();
        applications.deleteAll();
    }

    private UUID draft() {
        return draftService
                .saveNewDraft(
                        APPLICANT,
                        new DraftFields(
                                "ทดสอบ ผู้สมัคร",
                                35,
                                new BigDecimal("30000.00"),
                                24,
                                new BigDecimal("1000.00"),
                                new BigDecimal("100000.00"),
                                12))
                .getId();
    }

    /** Submitted with Band A/B moves straight on to UnderReview, and nobody is assigned yet. */
    private UUID underReviewUnassigned() {
        UUID id = draft();
        submitService.submit(id, APPLICANT);
        return id;
    }

    private UUID assignedTo(String officer) {
        UUID id = underReviewUnassigned();
        assignmentService.assign(id, officer, SUPERVISOR);
        return id;
    }

    /**
     * A Submitted application that never reached review — the state UC-miniloan-008's precondition
     * names. Nothing in the build plan produces one (submit assesses immediately), so the row is
     * driven to it directly: what is under test is who may cancel from Submitted, not how a row
     * comes to sit there.
     */
    private UUID forcedTo(UUID id, LoanApplication.Status status) {
        // Its own transaction, committed before the assertion runs: every other test here calls the
        // service the way a request does, with no ambient transaction to inherit.
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        tx ->
                                entityManager
                                        .createNativeQuery(
                                                "update loan_applications set status = ?1 where id = ?2")
                                        .setParameter(1, status.name())
                                        .setParameter(2, id)
                                        .executeUpdate());
        return id;
    }

    // ── the supervisor's half — unassigned (UC-miniloan-008 · ACL-007) ───────────

    /** AC-miniloan-090: Draft → Cancelled, the edge BR-miniloan-010@v1 declared, walked for real. */
    @Test
    void theSupervisorCancelsAnUnassignedDraftAndTheActorAndTimeAreRecorded() {
        UUID id = draft();

        var cancelled = cancellationService.cancel(id, REASON, SUPERVISOR);

        assertThat(cancelled.getStatus()).isEqualTo(LoanApplication.Status.Cancelled);
        assertThat(cancelled.getCancellationReason()).isEqualTo(REASON);
        assertThat(cancelled.getCancelledBy()).isEqualTo(SUPERVISOR);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getCancellationReason, LoanApplication::getCancelledBy)
                .containsExactly(REASON, SUPERVISOR);
    }

    /** The other state ACL-007 admits — a Submitted application still waiting on assessment. */
    @Test
    void theSupervisorCancelsAnUnassignedSubmittedApplication() {
        UUID id = forcedTo(draft(), LoanApplication.Status.Submitted);

        var cancelled = cancellationService.cancel(id, REASON, SUPERVISOR);

        assertThat(cancelled.getStatus()).isEqualTo(LoanApplication.Status.Cancelled);
    }

    /**
     * AC-miniloan-091 — the constraint @v1 did not have: any Loan Officer could cancel an
     * unassigned application under the old rule, and under @v2 none can.
     */
    @Test
    void aLoanOfficerMayNotCancelAnUnassignedApplication() {
        UUID id = forcedTo(draft(), LoanApplication.Status.Submitted);

        assertThatThrownBy(() -> cancellationService.cancel(id, REASON, OFFICER))
                .isInstanceOf(LoanApplicationCancellationService.SupervisorOnlyException.class)
                .hasMessage("ยกเลิกใบสมัครที่ยังไม่ถูกมอบหมายได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Submitted);
    }

    /**
     * The window the permission matrix leaves closed, reported rather than papered over: ACL-007's
     * scope ends at Submitted, so once assessment has moved the application to UnderReview the
     * supervisor cannot cancel it either — and nobody has been assigned to take over yet.
     */
    @Test
    void theSupervisorCannotCancelOnceTheApplicationReachedReviewUnassigned() {
        UUID id = underReviewUnassigned();

        assertThatThrownBy(() -> cancellationService.cancel(id, REASON, SUPERVISOR))
                .isInstanceOf(LoanApplicationCancellationService.SupervisorScopeEndedException.class);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    // ── the officer's half — assigned (UC-miniloan-007 · ACL-006) ────────────────

    /** AC-miniloan-067: the assigned officer cancels from UnderReview, reason kept on the row. */
    @Test
    void theAssignedOfficerCancelsFromUnderReview() {
        UUID id = assignedTo(OFFICER);

        var cancelled = cancellationService.cancel(id, REASON, OFFICER);

        assertThat(cancelled.getStatus()).isEqualTo(LoanApplication.Status.Cancelled);
        assertThat(cancelled.getCancelledBy()).isEqualTo(OFFICER);
        assertThat(cancelled.getCancellationReason()).isEqualTo(REASON);
    }

    /** AC-miniloan-047: Approved but not yet disbursed is still cancellable by the same officer. */
    @Test
    void theAssignedOfficerCancelsAnApprovedApplicationBeforeDisbursement() {
        UUID id = assignedTo(OFFICER);
        approvalService.approve(id, null, OFFICER);

        var cancelled = cancellationService.cancel(id, REASON, OFFICER);

        assertThat(cancelled.getStatus()).isEqualTo(LoanApplication.Status.Cancelled);
        assertThat(cancelled.getCancelledBy()).isEqualTo(OFFICER);
        // The approval record is not erased by the cancellation that followed it.
        assertThat(cancelled.getApprovedBy()).isEqualTo(OFFICER);
    }

    /**
     * AC-miniloan-092 — the boundary, and the whole point of BR-miniloan-031@v2: the same
     * supervisor who could have cancelled this application one moment earlier cannot now.
     */
    @Test
    void theSupervisorLosesTheRightTheMomentTheApplicationIsAssigned() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> cancellationService.cancel(id, REASON, SUPERVISOR))
                .isInstanceOf(LoanApplicationCancellationService.AssignedOfficerOnlyException.class)
                .hasMessage("ใบสมัครนี้ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะเจ้าหน้าที่ที่รับผิดชอบใบนี้");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** Holding ROLE-002 is not holding this application (AC-miniloan-065's rule on this action). */
    @Test
    void anOfficerItWasNotAssignedToMayNotCancel() {
        UUID id = assignedTo(ANOTHER_OFFICER);

        assertThatThrownBy(() -> cancellationService.cancel(id, REASON, OFFICER))
                .isInstanceOf(LoanApplicationCancellationService.AssignedOfficerOnlyException.class);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    // ── the reason (BR-miniloan-047@v1) ─────────────────────────────────────────

    /** AC-miniloan-068: the same standard as rejection, said so in the criterion itself. */
    @Test
    void cancellingWithNoReasonIsRefusedAndTheApplicationDoesNotMove() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> cancellationService.cancel(id, null, OFFICER))
                .isInstanceOf(LoanApplication.CancellationReasonRequiredException.class)
                .hasMessage("ยกเลิกไม่ได้ — ต้องระบุเหตุผลการยกเลิก");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus, LoanApplication::getCancellationReason)
                .containsExactly(LoanApplication.Status.UnderReview, null);
    }

    /**
     * AC-miniloan-068 measured where a rollback cannot answer for the guard: the service call is
     * @Transactional, so the row would read UnderReview afterwards whether the reason was checked
     * before the mutation or after it. Here the aggregate is held in memory with no transaction
     * anywhere near it — nothing was written, so there is nothing to roll back.
     */
    @Test
    void theAggregateItselfWritesNothingWhenTheCancellationReasonIsMissing() {
        LoanApplication application = new LoanApplication(APPLICANT);

        for (String nothing : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> application.cancel(nothing, SUPERVISOR))
                    .isInstanceOf(LoanApplication.CancellationReasonRequiredException.class)
                    .hasMessage("ยกเลิกไม่ได้ — ต้องระบุเหตุผลการยกเลิก");

            assertThat(application.getStatus()).isEqualTo(LoanApplication.Status.Draft);
            assertThat(application.getCancellationReason()).isNull();
            assertThat(application.getCancelledBy()).isNull();
            assertThat(application.getCancelledAt()).isNull();
        }
    }

    // ── the states with no edge out (BR-miniloan-010@v1) ────────────────────────

    /**
     * AC-miniloan-048 — the boundary where cancellation runs out of rights. Disbursed is produced by
     * FE-miniloan-010, which does not exist yet, so the row is driven to that status directly: the
     * guard is what is under test, and building the disbursement transition here would be doing
     * another unit's work inside this one.
     */
    @Test
    void aDisbursedApplicationCannotBeCancelledAndIsSentToAccountClosureInstead() {
        UUID id = assignedTo(OFFICER);
        approvalService.approve(id, null, OFFICER);
        forcedTo(id, LoanApplication.Status.Disbursed);

        assertThatThrownBy(() -> cancellationService.cancel(id, REASON, OFFICER))
                .isInstanceOf(LoanApplication.NotCancellableException.class)
                .hasMessage(
                        "ยกเลิกใบสมัครที่เบิกจ่ายแล้วไม่ได้ — ใบนี้มีบัญชีสินเชื่อเปิดอยู่ ให้ดำเนินการทางปิดบัญชีแทน");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Disbursed);
    }

    /** Rejected is final too, and the refusal names which ending this application already had. */
    @Test
    void aRejectedApplicationCannotBeCancelled() {
        UUID id = assignedTo(OFFICER);
        rejectionService.reject(id, "ภาระหนี้ต่อรายได้สูงเกินเกณฑ์", OFFICER);

        assertThatThrownBy(() -> cancellationService.cancel(id, REASON, OFFICER))
                .isInstanceOf(LoanApplication.NotCancellableException.class)
                .hasMessage("ยกเลิกไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้ว");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Rejected);
    }

    /** Cancelled has no edge back to itself; the second attempt quotes the first. */
    @Test
    void cancellingAnApplicationAlreadyCancelledIsRefused() {
        UUID id = assignedTo(OFFICER);
        cancellationService.cancel(id, REASON, OFFICER);
        var stored = applications.findById(id).orElseThrow();

        assertThatThrownBy(() -> cancellationService.cancel(id, "เหตุผลอื่น", OFFICER))
                .isInstanceOf(LoanApplication.NotCancellableException.class)
                .hasMessage("ยกเลิกไม่ได้ — ใบสมัครนี้ถูกยกเลิกไปแล้วเมื่อ " + stored.getCancelledAt());

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getCancellationReason)
                .isEqualTo(REASON);
    }

    /** BR-miniloan-043@v1 · AC-miniloan-134: the retry fence is a row the database refuses twice. */
    @Test
    void cancellingClaimsTheCommandKeyOnTheDatabase() {
        UUID id = assignedTo(OFFICER);

        cancellationService.cancel(id, REASON, OFFICER);

        assertThat(idempotencyKeys.findAll())
                .extracting(IdempotencyKey::getCommandType, IdempotencyKey::getRequestId)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(
                                IdempotencyKey.CommandType.CancelApplication, id.toString()));
    }
}

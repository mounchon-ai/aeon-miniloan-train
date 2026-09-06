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
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ปฏิเสธใบสมัคร (UC-miniloan-006 · BR-miniloan-013@v1 · AC-miniloan-055/056/057).
 *
 * <p>The demo Loan Officer's staff id is the resolved role "ROLE-002" (FE-miniloan-002's mock scheme
 * has one identity per role), so an application assigned to any other id is one this officer may not
 * act on — the same stand-in FE-miniloan-007 used for the approve half of this review.
 */
@SpringBootTest
class LoanApplicationRejectionServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OFFICER = "ROLE-002";
    private static final String ANOTHER_OFFICER = "STAFF-ข";
    private static final String REASON = "ภาระหนี้ต่อรายได้สูงเกินเกณฑ์";

    @Autowired private LoanApplicationRejectionService rejectionService;
    @Autowired private LoanApplicationApprovalService approvalService;
    @Autowired private ApplicationAssignmentService assignmentService;
    @Autowired private LoanApplicationSubmitService submitService;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private LoanApplicationRepository applications;
    @Autowired private ApplicationAssignmentRepository assignments;
    @Autowired private CreditAssessmentRepository assessments;
    @Autowired private IdempotencyKeyRepository idempotencyKeys;

    @BeforeEach
    void clean() {
        assignments.deleteAll();
        assessments.deleteAll();
        idempotencyKeys.deleteAll();
        applications.deleteAll();
    }

    private UUID submitted() {
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

    private UUID assignedTo(String officer) {
        UUID id = submitted();
        submitService.submit(id, APPLICANT);
        assignmentService.assign(id, officer, SUPERVISOR);
        return id;
    }

    /** AC-miniloan-055: rejected, with the reason, the officer and the moment on the row itself. */
    @Test
    void theAssignedOfficerRejectsAndTheReasonOfficerAndTimeAreStored() {
        UUID id = assignedTo(OFFICER);

        var rejected = rejectionService.reject(id, REASON, OFFICER);

        assertThat(rejected.getStatus()).isEqualTo(LoanApplication.Status.Rejected);
        assertThat(rejected.getRejectionReason()).isEqualTo(REASON);
        assertThat(rejected.getRejectedBy()).isEqualTo(OFFICER);
        assertThat(rejected.getRejectedAt()).isNotNull();
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getRejectionReason, LoanApplication::getRejectedBy)
                .containsExactly(REASON, OFFICER);
    }

    /** AC-miniloan-056: no reason, no transition — and the exact sentence the officer is shown. */
    @Test
    void rejectingWithNoReasonIsRefusedAndTheApplicationDoesNotMove() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> rejectionService.reject(id, null, OFFICER))
                .isInstanceOf(LoanApplication.RejectionReasonRequiredException.class)
                .hasMessage("ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus, LoanApplication::getRejectionReason)
                .containsExactly(LoanApplication.Status.UnderReview, null);
    }

    /**
     * AC-miniloan-056 again — "เว้นช่องเหตุผลไว้ว่าง" is what a form actually posts, and whitespace
     * is not a reason.
     */
    @Test
    void aBlankReasonIsTheSameAsNoReason() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> rejectionService.reject(id, "   ", OFFICER))
                .isInstanceOf(LoanApplication.RejectionReasonRequiredException.class)
                .hasMessage("ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /**
     * AC-miniloan-056 measured where the rollback cannot answer for the guard.
     *
     * <p>The two tests above go through {@code @Transactional} service calls, so the row reads
     * UnderReview afterwards whether the reason was checked before the mutation or after it — the
     * rollback would hide a half-applied rejection. This one holds the aggregate in memory, with no
     * repository and no transaction anywhere near it, and asks the object itself: nothing was
     * written, so there is nothing to roll back.
     */
    @Test
    void theAggregateItselfWritesNothingWhenTheReasonIsMissing() {
        LoanApplication application = new LoanApplication(APPLICANT);
        application.applyDraftFields(
                "ทดสอบ ผู้สมัคร",
                35,
                new BigDecimal("30000.00"),
                24,
                new BigDecimal("1000.00"),
                new BigDecimal("100000.00"),
                12);
        application.submit();
        application.moveToUnderReview();

        for (String nothing : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> application.reject(nothing, OFFICER))
                    .isInstanceOf(LoanApplication.RejectionReasonRequiredException.class)
                    .hasMessage("ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ");

            assertThat(application.getStatus()).isEqualTo(LoanApplication.Status.UnderReview);
            assertThat(application.getRejectionReason()).isNull();
            assertThat(application.getRejectedBy()).isNull();
            assertThat(application.getRejectedAt()).isNull();
        }
    }

    /**
     * AC-miniloan-057 — the boundary: Rejected is a final state in STM-miniloan-001, so the approve
     * edge is not there to take, and the refusal says which of the three reasons it is.
     */
    @Test
    void approvingAnApplicationAlreadyRejectedIsAnInvalidTransition() {
        UUID id = assignedTo(OFFICER);
        rejectionService.reject(id, REASON, OFFICER);

        assertThatThrownBy(() -> approvalService.approve(id, null, OFFICER))
                .isInstanceOf(LoanApplication.NotApprovableException.class)
                .hasMessage("อนุมัติไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้ว");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus, LoanApplication::getApprovedBy)
                .containsExactly(LoanApplication.Status.Rejected, null);
    }

    /** Rejected has no edge back to itself either — the second refusal names the first. */
    @Test
    void rejectingAnApplicationAlreadyRejectedIsRefused() {
        UUID id = assignedTo(OFFICER);
        rejectionService.reject(id, REASON, OFFICER);
        // The refusal quotes the row, and the row is what the database kept: a column holds
        // microseconds where the in-memory Instant still carries nanoseconds, so the expectation
        // has to be read back rather than taken from the object that was just saved.
        var stored = applications.findById(id).orElseThrow();

        assertThatThrownBy(() -> rejectionService.reject(id, "เหตุผลอื่น", OFFICER))
                .isInstanceOf(LoanApplication.NotRejectableException.class)
                .hasMessage("ปฏิเสธไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้วเมื่อ " + stored.getRejectedAt());

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getRejectionReason)
                .isEqualTo(REASON);
    }

    /** UC-miniloan-006's precondition — an application that never reached review has no such edge. */
    @Test
    void anApplicationStillInDraftCannotBeRejected() {
        UUID id = submitted();

        assertThatThrownBy(() -> rejectionService.reject(id, REASON, OFFICER))
                .isInstanceOf(LoanApplication.NotRejectableException.class)
                .hasMessage("ปฏิเสธไม่ได้ — ใบสมัครนี้ยังไม่เข้าสู่การพิจารณา");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Draft);
    }

    /** AC-miniloan-065's rule on this action — holding ROLE-002 is not holding this application. */
    @Test
    void anOfficerItWasNotAssignedToMayNotReject() {
        UUID id = assignedTo(ANOTHER_OFFICER);

        assertThatThrownBy(() -> rejectionService.reject(id, REASON, OFFICER))
                .isInstanceOf(LoanApplication.AssignedToAnotherOfficerException.class);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** AC-miniloan-066: nobody may act on an application no supervisor has handed on yet. */
    @Test
    void anUnassignedApplicationMayNotBeRejected() {
        UUID id = submitted();
        submitService.submit(id, APPLICANT);

        assertThatThrownBy(() -> rejectionService.reject(id, REASON, OFFICER))
                .isInstanceOf(LoanApplication.NotAssignedException.class);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /**
     * BR-miniloan-043@v1 · AC-miniloan-134: the retry fence is a row the database refuses to write
     * twice, not a disabled button. The state guard answers the sequential repeat first — this only
     * asserts that the claim was actually made, which is what separates two simultaneous calls.
     */
    @Test
    void rejectingClaimsTheCommandKeyOnTheDatabase() {
        UUID id = assignedTo(OFFICER);

        rejectionService.reject(id, REASON, OFFICER);

        assertThat(idempotencyKeys.findAll())
                .extracting(IdempotencyKey::getCommandType, IdempotencyKey::getRequestId)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(
                                IdempotencyKey.CommandType.RejectApplication, id.toString()));
    }
}

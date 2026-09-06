package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * อนุมัติใบสมัคร (UC-miniloan-005 · BR-miniloan-011@v1 · BR-miniloan-012@v1).
 *
 * <p>The demo Loan Officer's staff id is the resolved role "ROLE-002" (FE-miniloan-002's mock scheme
 * has one identity per role), so an application assigned to any other id is one this officer may not
 * act on — which is how AC-miniloan-114's "ข." is expressed with no scheme invented for it.
 */
@SpringBootTest
class LoanApplicationApprovalServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OFFICER = "ROLE-002";
    private static final String ANOTHER_OFFICER = "STAFF-ข";

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

    /**
     * Income 30,000 → max approvable 150,000 (BR-miniloan-003@v1), which is the ceiling
     * AC-miniloan-052/053/054 are all written against.
     */
    private UUID submitted(BigDecimal requestedAmount) {
        UUID id = draftService
                .saveNewDraft(
                        APPLICANT,
                        new DraftFields(
                                "ทดสอบ ผู้สมัคร",
                                35,
                                new BigDecimal("30000.00"),
                                24,
                                new BigDecimal("1000.00"),
                                requestedAmount,
                                12))
                .getId();
        submitService.submit(id, APPLICANT);
        return id;
    }

    private UUID assignedTo(String officer, BigDecimal requestedAmount) {
        UUID id = submitted(requestedAmount);
        assignmentService.assign(id, officer, SUPERVISOR);
        return id;
    }

    /** AC-miniloan-049 · AC-miniloan-113: approved, with the approver and the time on the row itself. */
    @Test
    void theAssignedOfficerApprovesAndTheApproverAndTimeAreStored() {
        UUID id = assignedTo(OFFICER, new BigDecimal("100000.00"));

        var approved = approvalService.approve(id, null, OFFICER);

        assertThat(approved.getStatus()).isEqualTo(LoanApplication.Status.Approved);
        assertThat(approved.getApprovedBy()).isEqualTo(OFFICER);
        assertThat(approved.getApprovedAt()).isNotNull();
        // Omitting the amount approves at what was asked for.
        assertThat(approved.getApprovedAmount()).isEqualByComparingTo("100000.00");
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getApprovedBy)
                .isEqualTo(OFFICER);
    }

    /** AC-miniloan-052: asking for exactly the ceiling is not over it. */
    @Test
    void requestingExactlyTheCeilingIsApprovedWithNoWarning() {
        UUID id = assignedTo(OFFICER, new BigDecimal("150000.00"));

        var approved = approvalService.approve(id, null, OFFICER);

        assertThat(approved.getStatus()).isEqualTo(LoanApplication.Status.Approved);
        assertThat(approved.getApprovedAmount()).isEqualByComparingTo("150000.00");
    }

    /** AC-miniloan-053: one baht over, refused with both figures named, and still UnderReview. */
    @Test
    void oneBahtOverTheCeilingIsRefusedAndTheApplicationDoesNotMove() {
        UUID id = assignedTo(OFFICER, new BigDecimal("150001.00"));

        assertThatThrownBy(() -> approvalService.approve(id, null, OFFICER))
                .isInstanceOf(LoanApplication.AmountExceedsMaxApprovableException.class)
                .hasMessage(
                        "อนุมัติไม่ได้ — จำนวนเงินที่ขอ 150,001 บาท เกินวงเงินอนุมัติสูงสุด 150,000 บาท กรุณาปรับวงเงินก่อน");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /**
     * AC-miniloan-054: the officer lowers the amount and it goes through — and the DTI does not move,
     * because it was computed from the requested amount at submission and the requested amount is
     * never overwritten (BR-miniloan-002@v1 · AC-miniloan-040).
     */
    @Test
    void loweringTheAmountLetsItThroughAndLeavesTheDtiAlone() {
        UUID id = assignedTo(OFFICER, new BigDecimal("150001.00"));
        BigDecimal dtiAtSubmission = assessments.findByApplicationId(id).orElseThrow().getDtiRatio();

        var approved = approvalService.approve(id, new BigDecimal("150000.00"), OFFICER);

        assertThat(approved.getStatus()).isEqualTo(LoanApplication.Status.Approved);
        assertThat(approved.getApprovedAmount()).isEqualByComparingTo("150000.00");
        assertThat(approved.getRequestedAmount()).isEqualByComparingTo("150001.00");
        assertThat(assessments.findByApplicationId(id).orElseThrow().getDtiRatio())
                .isEqualByComparingTo(dtiAtSubmission);
    }

    /** AC-miniloan-050: still being assessed means it has not reached review yet. */
    @Test
    void anApplicationThatHasNotReachedReviewCannotBeApproved() {
        // A Band C application stops at Submitted (FE-miniloan-005), which is that state.
        UUID id = draftService
                .saveNewDraft(
                        APPLICANT,
                        new DraftFields(
                                "ทดสอบ อายุน้อย",
                                19,
                                new BigDecimal("30000.00"),
                                24,
                                new BigDecimal("1000.00"),
                                new BigDecimal("100000.00"),
                                12))
                .getId();
        submitService.submit(id, APPLICANT);

        assertThatThrownBy(() -> approvalService.approve(id, null, OFFICER))
                .isInstanceOf(LoanApplication.NotApprovableException.class)
                .hasMessage("อนุมัติไม่ได้ — ใบสมัครนี้ยังไม่เข้าสู่การพิจารณา");

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Submitted);
        assertThat(applications.findById(id)).get().extracting(LoanApplication::getApprovedBy).isNull();
    }

    /** AC-miniloan-051: approving twice keeps the first approver and time, unchanged. */
    @Test
    void approvingTwiceDoesNotOverwriteTheFirstApproval() {
        UUID id = assignedTo(OFFICER, new BigDecimal("100000.00"));
        approvalService.approve(id, null, OFFICER);
        LoanApplication first = applications.findById(id).orElseThrow();

        assertThatThrownBy(() -> approvalService.approve(id, null, OFFICER))
                .isInstanceOf(LoanApplication.NotApprovableException.class)
                .hasMessageStartingWith("อนุมัติไม่ได้ — ใบสมัครนี้อนุมัติไปแล้วเมื่อ");

        LoanApplication after = applications.findById(id).orElseThrow();
        assertThat(after.getApprovedBy()).isEqualTo(first.getApprovedBy());
        assertThat(after.getApprovedAt()).isEqualTo(first.getApprovedAt());
        assertThat(after.getApprovedAmount()).isEqualByComparingTo(first.getApprovedAmount());
        // BR-miniloan-043@v1's key was claimed once, by the approval that actually happened.
        assertThat(idempotencyKeys.count()).isEqualTo(2); // submit + approve, one each
    }

    /** AC-miniloan-114: assigned to somebody else, so this officer is refused at the rule, not the UI. */
    @Test
    void anOfficerTheApplicationWasNotAssignedToIsRefused() {
        UUID id = assignedTo(ANOTHER_OFFICER, new BigDecimal("100000.00"));

        assertThatThrownBy(() -> approvalService.approve(id, null, OFFICER))
                .isInstanceOf(LoanApplication.AssignedToAnotherOfficerException.class);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** AC-miniloan-066: nobody may approve an application no supervisor has handed on. */
    @Test
    void anUnassignedApplicationCannotBeApproved() {
        UUID id = submitted(new BigDecimal("100000.00"));

        assertThatThrownBy(() -> approvalService.approve(id, null, OFFICER))
                .isInstanceOf(LoanApplication.NotAssignedException.class);
    }

    /**
     * AC-miniloan-045: the main path walks Draft → Submitted → UnderReview → Approved with no step
     * skipped, each change carrying who did it and when. Disbursed is the last hop and belongs to
     * FE-miniloan-010.
     */
    @Test
    void theMainPathWalksEveryStateInOrder() {
        UUID id = draftService
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
        assertThat(applications.findById(id).orElseThrow().getStatus())
                .isEqualTo(LoanApplication.Status.Draft);

        submitService.submit(id, APPLICANT);
        LoanApplication assessed = applications.findById(id).orElseThrow();
        assertThat(assessed.getStatus()).isEqualTo(LoanApplication.Status.UnderReview);
        assertThat(assessed.getSubmittedAt()).isNotNull();

        assignmentService.assign(id, OFFICER, SUPERVISOR);
        var approved = approvalService.approve(id, null, OFFICER);

        assertThat(approved.getStatus()).isEqualTo(LoanApplication.Status.Approved);
        assertThat(approved.getApprovedBy()).isEqualTo(OFFICER);
        assertThat(assignmentService.latest(id).orElseThrow().getAssignedBy()).isEqualTo(SUPERVISOR);
    }

    /** AC-miniloan-046: there is no way back, and no method on the aggregate offers one. */
    @Test
    void anApprovedApplicationCannotBeWalkedBackwards() {
        UUID id = assignedTo(OFFICER, new BigDecimal("100000.00"));
        approvalService.approve(id, null, OFFICER);
        LoanApplication approved = applications.findById(id).orElseThrow();

        assertThatThrownBy(approved::moveToUnderReview)
                .isInstanceOf(LoanApplication.IllegalStateTransitionException.class);
        assertThatThrownBy(approved::submit)
                .isInstanceOf(LoanApplication.IllegalStateTransitionException.class);
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Approved);
    }
}

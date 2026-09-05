package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.miniloan.domain.ApplicationAssignment;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.service.ApplicationAssignmentService.LoanOfficerRequiredException;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * BR-miniloan-032@v1 · UC-miniloan-004. AC-miniloan-064/065/066 all describe what happens when an
 * officer then presses approve or reject, and those endpoints are FE-miniloan-007/008 — what is
 * provable here is the rule they will call, exercised straight on the aggregate with two distinct
 * officer ids. That is also the only way it can be exercised today: the mock-token scheme resolves
 * one identity per role (FE-miniloan-002), so "ก." and "ข." cannot be two different callers.
 */
@SpringBootTest
class ApplicationAssignmentServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OFFICER_KOR = "STAFF-ก";
    private static final String OFFICER_KHOR = "STAFF-ข";

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

    /** An application that has been through submit and its assessment, so it sits at UnderReview. */
    private UUID underReview() {
        UUID id = draftService
                .saveNewDraft(
                        APPLICANT,
                        new DraftFields(
                                "ทดสอบ ผู้สมัคร",
                                35,
                                new BigDecimal("30000.00"),
                                24,
                                new BigDecimal("10500.00"),
                                new BigDecimal("100000.00"),
                                12))
                .getId();
        var submitted = submitService.submit(id, APPLICANT);
        assertThat(submitted.application().getStatus()).isEqualTo(LoanApplication.Status.UnderReview);
        return id;
    }

    /** AC-miniloan-064: the supervisor hands it over, and the record says who, by whom, and when. */
    @Test
    void theSupervisorAssignsOneOfficerAndTheHandoverIsRecorded() {
        UUID id = underReview();

        var result = assignmentService.assign(id, OFFICER_KOR, SUPERVISOR);

        assertThat(result.application().getAssignedLoanOfficerId()).isEqualTo(OFFICER_KOR);
        assertThat(result.assignment().getAssignedBy()).isEqualTo(SUPERVISOR);
        assertThat(result.assignment().getAssignedAt()).isNotNull();
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getAssignedLoanOfficerId)
                .isEqualTo(OFFICER_KOR);
    }

    /**
     * AC-miniloan-065: same role, same rights, still refused — only the assigned officer may act.
     * The guard is on the aggregate so approve, reject and cancel all reach it.
     */
    @Test
    void anotherOfficerWithTheSameRoleIsStillRefused() {
        UUID id = underReview();
        assignmentService.assign(id, OFFICER_KOR, SUPERVISOR);
        LoanApplication application = applications.findById(id).orElseThrow();

        assertThatCode(() -> application.requireAssignedTo(OFFICER_KOR)).doesNotThrowAnyException();
        assertThatThrownBy(() -> application.requireAssignedTo(OFFICER_KHOR))
                .isInstanceOf(LoanApplication.AssignedToAnotherOfficerException.class)
                .hasMessageContaining(OFFICER_KOR);
    }

    /** AC-miniloan-066: unassigned refuses everyone, and that is the normal state, not a fault. */
    @Test
    void anUnassignedApplicationRefusesEveryOfficer() {
        UUID id = underReview();
        LoanApplication application = applications.findById(id).orElseThrow();

        assertThat(application.getAssignedLoanOfficerId()).isNull();
        assertThatThrownBy(() -> application.requireAssignedTo(OFFICER_KOR))
                .isInstanceOf(LoanApplication.NotAssignedException.class);
        assertThatThrownBy(() -> application.requireAssignedTo(OFFICER_KHOR))
                .isInstanceOf(LoanApplication.NotAssignedException.class);
    }

    /**
     * ENT-013 keeps every round ("แถวใหม่ถูกเพิ่มทุกครั้งที่มอบหมายใหม่ ไม่ทับแถวเดิม"), while the
     * application itself points at the latest officer only. Re-assignment being allowed was decided
     * with the project owner on 2026-09-05, against UC-miniloan-004's main-flow precondition.
     */
    @Test
    void reassigningAppendsToTheHistoryAndMovesThePointer() {
        UUID id = underReview();
        assignmentService.assign(id, OFFICER_KOR, SUPERVISOR);

        assignmentService.assign(id, OFFICER_KHOR, SUPERVISOR);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getAssignedLoanOfficerId)
                .isEqualTo(OFFICER_KHOR);
        assertThat(assignmentService.history(id))
                .extracting(ApplicationAssignment::getLoanOfficerId)
                .containsExactly(OFFICER_KOR, OFFICER_KHOR);
    }

    /** ACL-003's condition: UnderReview only — a Submitted application is not ready to be handed on. */
    @Test
    void anApplicationOutsideUnderReviewCannotBeAssigned() {
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
        // Band C stops at Submitted (FE-miniloan-005), which is exactly the state under test.
        submitService.submit(id, APPLICANT);

        assertThatThrownBy(() -> assignmentService.assign(id, OFFICER_KOR, SUPERVISOR))
                .isInstanceOf(LoanApplication.NotAssignableException.class);
        assertThat(assignmentService.history(id)).isEmpty();
    }

    /**
     * AC-miniloan-064's second half — "ผู้รับผิดชอบ: ก. (มอบหมายโดย … เมื่อ …)". The application page
     * reads it from API-004, so the three values have to leave the API together or apps/web cannot
     * render the line at all (REQ-miniloan-006: the client computes nothing).
     */
    @Test
    void theDetailViewCarriesWhoHoldsItWhoHandedItOverAndWhen() {
        UUID id = underReview();
        var assigned = assignmentService.assign(id, OFFICER_KOR, SUPERVISOR);

        var latest = assignmentService.latest(id).orElseThrow();

        assertThat(latest.getId()).isEqualTo(assigned.assignment().getId());
        assertThat(latest.getLoanOfficerId()).isEqualTo(OFFICER_KOR);
        assertThat(latest.getAssignedBy()).isEqualTo(SUPERVISOR);
        // Read back through JDBC, so the instant is compared at the column's precision rather than
        // the in-memory object's nanoseconds — the claim is that the value travels, not how finely.
        assertThat(latest.getAssignedAt())
                .isCloseTo(assigned.assignment().getAssignedAt(), within(1, ChronoUnit.MILLIS));
    }

    /** BR-miniloan-032@v1 asks for one named officer — a blank name is not an assignment. */
    @Test
    void assigningNobodyIsRefused() {
        UUID id = underReview();

        assertThatThrownBy(() -> assignmentService.assign(id, "  ", SUPERVISOR))
                .isInstanceOf(LoanOfficerRequiredException.class);
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getAssignedLoanOfficerId)
                .isNull();
    }
}

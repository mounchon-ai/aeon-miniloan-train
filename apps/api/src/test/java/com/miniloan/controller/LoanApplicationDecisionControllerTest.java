package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.controller.LoanApplicationDecisionController.ApproveRequest;
import com.miniloan.controller.LoanApplicationDecisionController.RejectRequest;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.service.ApplicationAssignmentService;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationSubmitService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * BR-miniloan-025@v1 at the layer it names: the route refuses on its own, so a caller who never
 * loads a screen gets the same answer the screen would have given (UC-miniloan-021).
 */
@SpringBootTest
class LoanApplicationDecisionControllerTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String REASON = "ภาระหนี้ต่อรายได้สูงเกินเกณฑ์";

    @Autowired private LoanApplicationDecisionController controller;
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

    private static MockHttpServletRequest as(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE, role);
        return request;
    }

    private UUID assignedTo(String officer) {
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
        submitService.submit(id, APPLICANT);
        assignmentService.assign(id, officer, SUPERVISOR);
        return id;
    }

    /** AC-miniloan-113: the endpoint exists and does the whole job when called straight. */
    @Test
    void theAssignedOfficerMayApproveThroughTheRouteAlone() {
        UUID id = assignedTo(OFFICER);

        var response = controller.approve(id, null, as(OFFICER));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("Approved");
        assertThat(response.getBody().approvedBy()).isEqualTo(OFFICER);
        assertThat(response.getBody().approvedAt()).isNotNull();
    }

    /** AC-miniloan-062: the applicant calling their own application's approve route is refused here. */
    @Test
    void anApplicantCallingApproveDirectlyIsRefusedByTheApi() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> controller.approve(id, new ApproveRequest(null), as(APPLICANT)))
                .isInstanceOf(LoanApplicationDecisionController.ForbiddenRoleException.class);

        assertThat(controller.handleForbiddenRole().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.handleForbiddenRole().getBody().message()).isEqualTo("ไม่มีสิทธิ์อนุมัติใบสมัคร");
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** Nobody but a Loan Officer holds this edge (BR-miniloan-031@v2 · rbac defaults to deny). */
    @Test
    void noOtherRoleMayApprove() {
        UUID id = assignedTo(OFFICER);

        for (String role : new String[] {SUPERVISOR, "ROLE-004", "ROLE-005"}) {
            assertThatThrownBy(() -> controller.approve(id, null, as(role)))
                    .isInstanceOf(LoanApplicationDecisionController.ForbiddenRoleException.class);
        }
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** AC-miniloan-114 as the caller sees it — a 403 naming the reason, from the API itself. */
    @Test
    void anOfficerItWasNotAssignedToGetsTheApiRefusal() {
        UUID id = assignedTo("STAFF-ข");

        assertThatThrownBy(() -> controller.approve(id, null, as(OFFICER)))
                .isInstanceOf(LoanApplication.AssignedToAnotherOfficerException.class);

        var refusal = controller.handleAssignedToAnother();
        assertThat(refusal.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refusal.getBody().message()).isEqualTo("อนุมัติไม่ได้ — ใบสมัครนี้มอบหมายให้ผู้พิจารณาคนอื่น");
    }

    /** AC-miniloan-055 through the route alone — API-008 does the whole job with no screen. */
    @Test
    void theAssignedOfficerMayRejectThroughTheRouteAlone() {
        UUID id = assignedTo(OFFICER);

        var response = controller.reject(id, new RejectRequest(REASON), as(OFFICER));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("Rejected");
        assertThat(response.getBody().rejectionReason()).isEqualTo(REASON);
        assertThat(response.getBody().rejectedBy()).isEqualTo(OFFICER);
        assertThat(response.getBody().rejectedAt()).isNotNull();
    }

    /**
     * AC-miniloan-057 names both doors — "ทั้งจากหน้าจอและด้วยการเรียก API ตรง" — so the boundary is
     * measured here as well as on the service: the approve route refuses a Rejected application and
     * says why, and the application does not move.
     */
    @Test
    void approvingThroughTheRouteAfterARejectionIsRefused() {
        UUID id = assignedTo(OFFICER);
        controller.reject(id, new RejectRequest(REASON), as(OFFICER));

        assertThatThrownBy(() -> controller.approve(id, null, as(OFFICER)))
                .isInstanceOf(LoanApplication.NotApprovableException.class)
                .hasMessage("อนุมัติไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้ว");

        var refusal = controller.handleNotApprovable(
                new LoanApplication.NotApprovableException(applications.findById(id).orElseThrow()));
        assertThat(refusal.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refusal.getBody().message()).isEqualTo("อนุมัติไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้ว");
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Rejected);
    }

    /** AC-miniloan-056 at the API: an empty reason is turned down before anything is written. */
    @Test
    void rejectingWithABlankReasonIsRefusedByTheApi() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> controller.reject(id, new RejectRequest("  "), as(OFFICER)))
                .isInstanceOf(LoanApplication.RejectionReasonRequiredException.class);

        var refusal = controller.handleReasonRequired(new LoanApplication.RejectionReasonRequiredException());
        assertThat(refusal.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refusal.getBody().message()).isEqualTo("ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ");
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** No body at all is the same silence as an empty box (API-008 declares no request shape). */
    @Test
    void rejectingWithNoBodyIsRefusedByTheApi() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> controller.reject(id, null, as(OFFICER)))
                .isInstanceOf(LoanApplication.RejectionReasonRequiredException.class);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** BR-miniloan-025@v1: the applicant calling the reject route directly is refused by the API. */
    @Test
    void anApplicantCallingRejectDirectlyIsRefusedByTheApi() {
        UUID id = assignedTo(OFFICER);

        assertThatThrownBy(() -> controller.reject(id, new RejectRequest(REASON), as(APPLICANT)))
                .isInstanceOf(LoanApplicationDecisionController.RejectForbiddenRoleException.class);

        var refusal = controller.handleRejectForbiddenRole();
        assertThat(refusal.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refusal.getBody().message()).isEqualTo("ไม่มีสิทธิ์ปฏิเสธใบสมัคร");
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /** Nobody but a Loan Officer holds this edge either (rbac defaults to deny). */
    @Test
    void noOtherRoleMayReject() {
        UUID id = assignedTo(OFFICER);

        for (String role : new String[] {SUPERVISOR, "ROLE-004", "ROLE-005"}) {
            assertThatThrownBy(() -> controller.reject(id, new RejectRequest(REASON), as(role)))
                    .isInstanceOf(LoanApplicationDecisionController.RejectForbiddenRoleException.class);
        }
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /**
     * BR-miniloan-032@v1 as the caller of THIS action sees it — the shared domain guard is re-named
     * by the action, so a reject refusal never opens with "อนุมัติไม่ได้".
     */
    @Test
    void anOfficerItWasNotAssignedToGetsTheRejectApiRefusal() {
        UUID id = assignedTo("STAFF-ข");

        assertThatThrownBy(() -> controller.reject(id, new RejectRequest(REASON), as(OFFICER)))
                .isInstanceOf(LoanApplicationDecisionController.RejectAssignedToAnotherException.class);

        var refusal = controller.handleRejectAssignedToAnother();
        assertThat(refusal.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refusal.getBody().message()).isEqualTo("ปฏิเสธไม่ได้ — ใบสมัครนี้มอบหมายให้ผู้พิจารณาคนอื่น");
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }
}

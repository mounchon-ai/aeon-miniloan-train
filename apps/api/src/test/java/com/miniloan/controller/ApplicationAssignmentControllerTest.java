package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.controller.ApplicationAssignmentController.AssignRequest;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
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
 * BR-miniloan-032@v1's opening clause — "หัวหน้าเป็นผู้สั่งมอบหมาย … เจ้าหน้าที่หยิบงานเองไม่ได้" —
 * at the layer interfaces.json asks for it (ruleEnforcement: api, domain). The domain half lives in
 * {@code ApplicationAssignmentServiceTest}; this is the api half, and the refusal is the route's, not
 * a hidden button's (BR-miniloan-025@v1).
 */
@SpringBootTest
class ApplicationAssignmentControllerTest {

    @Autowired private ApplicationAssignmentController controller;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private LoanApplicationSubmitService submitService;
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

    private UUID underReview() {
        UUID id = draftService
                .saveNewDraft(
                        "ROLE-001",
                        new DraftFields(
                                "ทดสอบ ผู้สมัคร",
                                35,
                                new BigDecimal("30000.00"),
                                24,
                                new BigDecimal("10500.00"),
                                new BigDecimal("100000.00"),
                                12))
                .getId();
        submitService.submit(id, "ROLE-001");
        return id;
    }

    /** ACL-003: the supervisor, over any application (scope=all). */
    @Test
    void theSupervisorMayAssign() {
        UUID id = underReview();

        var response = controller.assign(id, new AssignRequest("STAFF-ก"), as("ROLE-003"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().assignedLoanOfficerId()).isEqualTo("STAFF-ก");
        assertThat(response.getBody().assignedBy()).isEqualTo("ROLE-003");
    }

    /** A Loan Officer taking work off the queue is the thing BR-miniloan-032@v1 forbids. */
    @Test
    void aLoanOfficerCannotAssignWorkToThemselves() {
        UUID id = underReview();

        assertThatThrownBy(() -> controller.assign(id, new AssignRequest("STAFF-ก"), as("ROLE-002")))
                .isInstanceOf(ApplicationAssignmentController.ForbiddenRoleException.class);

        assertThat(controller.handleForbiddenRole().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getAssignedLoanOfficerId)
                .isNull();
    }

    /** Nobody else either — rbac.json grants UC-miniloan-004 to ROLE-003 alone, and denies by default. */
    @Test
    void noOtherRoleMayAssign() {
        UUID id = underReview();

        for (String role : new String[] {"ROLE-001", "ROLE-004", "ROLE-005"}) {
            assertThatThrownBy(() -> controller.assign(id, new AssignRequest("STAFF-ก"), as(role)))
                    .isInstanceOf(ApplicationAssignmentController.ForbiddenRoleException.class);
        }
        assertThat(assignments.count()).isZero();
    }
}

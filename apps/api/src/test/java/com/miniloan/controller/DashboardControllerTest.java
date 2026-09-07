package com.miniloan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationSubmitService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API-020 (GET /dashboard) at the layer ACL-018 names alongside the domain, for UC-miniloan-020.
 *
 * <p><b>What only this can prove.</b> {@code DashboardSummaryServiceTest} measures the counting;
 * nothing in it would notice if the route were registered at the wrong path or verb, and
 * {@code ContractOpenApiTest} (FE-miniloan-003) enumerates no real route — it proves the springdoc
 * mechanism against a test-only fixture controller. A caller who meets a 404 has met the ABSENCE of
 * a route, not the enforcement of a rule, and every service-level test would still be green.
 *
 * <p><b>The refusal is measured over HTTP too.</b> AC-miniloan-112 leaves the data scope open but
 * rbac.json is default-deny and ACL-018 names ROLE-002 alone, so a role with no entry must be turned
 * down by the API itself rather than by a screen that declines to draw the page — the same reading
 * AC-miniloan-083 asks for on the settings route.
 *
 * <p>{@link AuthTokenFilter} gates every request with no exemptions (FE-miniloan-002), so the token
 * rides on each call exactly as apps/web's interceptor attaches it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    private static final String PATH = "/dashboard";

    /** FE-miniloan-002's scheme — one identity per role. */
    private static final String LOAN_OFFICER_TOKEN = "Bearer mock-role-002";

    private static final String APPLICANT_TOKEN = "Bearer mock-role-001";
    private static final String SUPERVISOR_TOKEN = "Bearer mock-role-003";

    @Autowired private MockMvc mockMvc;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private LoanApplicationSubmitService submitService;
    @Autowired private LoanApplicationRepository applications;
    @Autowired private LoanAccountRepository accounts;
    @Autowired private InterestRateVersionRepository rateVersions;
    @Autowired private RepaymentScheduleRepository schedules;
    @Autowired private InstallmentRepository installments;
    @Autowired private ApplicationAssignmentRepository assignments;
    @Autowired private CreditAssessmentRepository assessments;
    @Autowired private IdempotencyKeyRepository idempotencyKeys;

    @BeforeEach
    void clean() {
        installments.deleteAll();
        schedules.deleteAll();
        accounts.deleteAll();
        assignments.deleteAll();
        assessments.deleteAll();
        idempotencyKeys.deleteAll();
        applications.deleteAll();
        rateVersions.deleteAll();
    }

    /**
     * AC-miniloan-111 over the real route — an empty system answers 200 with five zeros, not an error
     * and not a body with fields missing. Every square is asserted by name, because a response that
     * omitted one would still be valid JSON.
     */
    @Test
    void anEmptySystemAnswersWithFiveZerosOverTheApi() throws Exception {
        mockMvc
                .perform(get(PATH).header("Authorization", LOAN_OFFICER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submittedApplications").value(0))
                .andExpect(jsonPath("$.underReviewApplications").value(0))
                .andExpect(jsonPath("$.approvedApplications").value(0))
                .andExpect(jsonPath("$.activeLoanAccounts").value(0))
                .andExpect(jsonPath("$.closedLoanAccounts").value(0));
    }

    /** The route really counts what is on disk — one Band C application lands in "ยื่นแล้ว" and nowhere else. */
    @Test
    void theRouteReportsWhatIsActuallyStored() throws Exception {
        UUID id =
                draftService
                        .saveNewDraft(
                                "ROLE-001",
                                new DraftFields(
                                        "ทดสอบ ผู้สมัคร",
                                        35,
                                        new BigDecimal("30000.00"),
                                        24,
                                        new BigDecimal("15000.00"),
                                        new BigDecimal("100000.00"),
                                        12))
                        .getId();
        submitService.submit(id, "ROLE-001");

        mockMvc
                .perform(get(PATH).header("Authorization", LOAN_OFFICER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submittedApplications").value(1))
                .andExpect(jsonPath("$.underReviewApplications").value(0));
    }

    /** ACL-018's api half — default-deny answers 403 with a code, never 200 with five zeros. */
    @Test
    void aRoleWithNoEntryIsRefusedByTheApiItself() throws Exception {
        mockMvc
                .perform(get(PATH).header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DASHBOARD_NOT_PERMITTED"));

        mockMvc
                .perform(get(PATH).header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DASHBOARD_NOT_PERMITTED"));
    }

    /** BR-miniloan-030@v1 — the token filter runs before the route, with no exemption for this one. */
    @Test
    void anUnauthenticatedCallNeverReachesTheDashboard() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }
}

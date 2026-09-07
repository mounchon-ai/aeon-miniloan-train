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
import java.math.BigDecimal;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API-005 (GET /applications) and AC-miniloan-127's API half, for UC-miniloan-023.
 *
 * <p><b>The criterion asks for this layer by name.</b> "การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ไม่แสดงลิงก์
 * บนหน้าจอ — ถ้ากันแค่บนหน้าจอ ใบนี้จะผ่าน" cannot be satisfied by a service test, and neither can
 * "ก. เรียก API ขอรายการใบสมัครทั้งหมดที่ตัวเองเข้าถึงได้": a route registered at the wrong path
 * answers 404, which is the ABSENCE of a route rather than the enforcement of a rule, and every
 * service-level test would still be green. {@code ContractOpenApiTest} (FE-miniloan-003) proves the
 * springdoc mechanism against a test-only fixture controller and enumerates no real route.
 *
 * <p>{@link AuthTokenFilter} gates every request with no exemptions (FE-miniloan-002), so the token
 * rides on each call exactly as apps/web's interceptor attaches it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScopedListingControllerTest {

    private static final String APPLICATIONS = "/applications";
    private static final String LOAN_ACCOUNTS = "/loan-accounts";

    /** FE-miniloan-002's scheme — one identity per role. */
    private static final String APPLICANT_TOKEN = "Bearer mock-role-001";

    private static final String OFFICER_TOKEN = "Bearer mock-role-002";
    private static final String SUPERVISOR_TOKEN = "Bearer mock-role-003";

    /** There is no second Applicant token, so "ผู้สมัคร ข." is a different applicant id. */
    private static final String ANOTHER_APPLICANT = "ROLE-001-another-person";

    @Autowired private MockMvc mockMvc;
    @Autowired private LoanApplicationDraftService draftService;
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
     * AC-miniloan-128 over the real route — two of the caller's applications against three of a
     * stranger's. The length is asserted, not only the membership.
     */
    @Test
    void theListRouteReturnsExactlyTheCallersOwnApplications() throws Exception {
        UUID mineFirst = draftFor("ROLE-001");
        UUID mineSecond = draftFor("ROLE-001");
        draftFor(ANOTHER_APPLICANT);
        draftFor(ANOTHER_APPLICANT);
        draftFor(ANOTHER_APPLICANT);

        mockMvc
                .perform(get(APPLICATIONS).header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(mineFirst.toString()))
                .andExpect(jsonPath("$[1].id").value(mineSecond.toString()));
    }

    /** ACL-003 · ACL-031 scope: all — the same table, every row. */
    @Test
    void theSupervisorGetsEveryApplicationFromTheSameRoute() throws Exception {
        draftFor("ROLE-001");
        draftFor(ANOTHER_APPLICANT);
        draftFor(ANOTHER_APPLICANT);

        mockMvc
                .perform(get(APPLICATIONS).header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(3)));
    }

    /** Default-deny on API-005 — a role with no declared list gets a refusal, not an empty array. */
    @Test
    void aRoleWithNoDeclaredListIsRefusedByTheRoute() throws Exception {
        draftFor("ROLE-001");

        mockMvc
                .perform(get(APPLICATIONS).header("Authorization", OFFICER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPLICATION_LIST_FORBIDDEN"));
    }

    /**
     * AC-miniloan-127, word for word, at the layer it names — the caller never loads a screen, and
     * the id is a real application that belongs to somebody else.
     */
    @Test
    void openingAnotherApplicantsApplicationByIdIsRefusedByTheApi() throws Exception {
        UUID theirs = draftFor(ANOTHER_APPLICANT);

        mockMvc
                .perform(get(APPLICATIONS + "/" + theirs).header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_VISIBLE"))
                .andExpect(jsonPath("$.message").value("ไม่มีสิทธิ์เข้าถึงใบสมัครนี้"));
    }

    /** The same answer for an id nobody owns, so the refusal cannot be used to enumerate ids. */
    @Test
    void anIdThatBelongsToNobodyGetsTheSameRefusalOverHttp() throws Exception {
        mockMvc
                .perform(get(APPLICATIONS + "/" + UUID.randomUUID()).header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_VISIBLE"))
                .andExpect(jsonPath("$.message").value("ไม่มีสิทธิ์เข้าถึงใบสมัครนี้"));
    }

    /**
     * The account half of AC-miniloan-126 on the route API-013 already declares — with no account of
     * the caller's own in the table, the list is empty rather than everybody's.
     */
    @Test
    void theAccountListRouteIsScopedToTheCallerToo() throws Exception {
        draftFor("ROLE-001");

        mockMvc
                .perform(get(LOAN_ACCOUNTS).header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    /** BR-miniloan-030@v1 — the token filter runs before the route, with no exemption for this one. */
    @Test
    void anUnauthenticatedCallNeverReachesTheList() throws Exception {
        mockMvc.perform(get(APPLICATIONS)).andExpect(status().isUnauthorized());
    }

    private UUID draftFor(String applicantId) {
        return draftService
                .saveNewDraft(
                        applicantId,
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
}

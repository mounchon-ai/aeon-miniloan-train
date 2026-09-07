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
import com.miniloan.service.ApplicationAssignmentService;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationSubmitService;
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

    private static final String OPERATIONS_TOKEN = "Bearer mock-role-004";

    /** ACL-027's "another officer" — one role, several people. */
    private static final String ANOTHER_OFFICER = "ROLE-002-another-person";

    /** There is no second Applicant token, so "ผู้สมัคร ข." is a different applicant id. */
    private static final String ANOTHER_APPLICANT = "ROLE-001-another-person";

    @Autowired private MockMvc mockMvc;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private LoanApplicationSubmitService submitService;
    @Autowired private ApplicationAssignmentService assignmentService;
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

    /**
     * Default-deny on API-005 — a role with no declared list gets a refusal, not an empty array.
     *
     * <p>This test used to send OFFICER_TOKEN. ROLE-002 was never a role rbac.json denies: ACL-027
     * grants it {@code scope: own} and the branch was simply unbuilt until FE-miniloan-023 built the
     * queue it serves. ROLE-004 has no application-list ACL at all and is what default-deny means.
     */
    @Test
    void aRoleWithNoDeclaredListIsRefusedByTheRoute() throws Exception {
        draftFor("ROLE-001");

        mockMvc
                .perform(get(APPLICATIONS).header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPLICATION_LIST_FORBIDDEN"));
    }

    /**
     * ACL-027 over the real route — UI-miniloan-006's queue. Two applications are assigned, one to
     * this officer and one to another person in the same role, and a third is assigned to nobody.
     * The size is asserted: a route that returned all three would still "contain the officer's work".
     */
    @Test
    void theQueueRouteReturnsOnlyTheApplicationsAssignedToTheCallingOfficer() throws Exception {
        UUID assignedToMe = submittedAndAssigned("ROLE-001", "ROLE-002");
        submittedAndAssigned(ANOTHER_APPLICANT, ANOTHER_OFFICER);
        draftFor(ANOTHER_APPLICANT);

        mockMvc
                .perform(get(APPLICATIONS).header("Authorization", OFFICER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(assignedToMe.toString()))
                // ENT-003's band travels with the row — UI-miniloan-006 shows it on every line, and
                // without it the queue would need one detail call per application to render a column.
                .andExpect(jsonPath("$[0].band").value(Matchers.notNullValue()));
    }

    /**
     * ENT-002's assignedLoanOfficerId travels with the row too (FE-miniloan-024) — UI-miniloan-010 is
     * the queue of applications nobody holds yet, and without this field the page cannot tell an
     * unassigned row from an assigned one. Asserted on the supervisor's list, where both kinds sit
     * side by side, because that is the only place the difference is visible.
     */
    @Test
    void theSupervisorListSaysWhichApplicationsAreAlreadyAssigned() throws Exception {
        UUID assigned = submittedAndAssigned("ROLE-001", "ROLE-002");
        UUID unassigned = draftFor(ANOTHER_APPLICANT);

        mockMvc
                .perform(get(APPLICATIONS).header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[?(@.id=='" + assigned + "')].assignedLoanOfficerId").value("ROLE-002"))
                // The filter expression yields a LIST, so the matcher has to be about the list: a
                // bare nullValue() compares [null] against null and fails while the field really is
                // null. contains() says "one element, and it is null", which is the fact meant here.
                .andExpect(
                        jsonPath("$[?(@.id=='" + unassigned + "')].assignedLoanOfficerId")
                                .value(Matchers.contains(Matchers.nullValue())));
    }

    /**
     * The other half of that field: a draft has no assessment (AC-miniloan-035), so its band is null
     * rather than a value that looks like a verdict nobody reached. Asserted on the applicant's own
     * list, where drafts live.
     */
    @Test
    void anUnassessedApplicationCarriesNoBand() throws Exception {
        draftFor("ROLE-001");

        mockMvc
                .perform(get(APPLICATIONS).header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].band").value(Matchers.nullValue()));
    }

    /**
     * ACL-028 over the real route — UI-miniloan-007's unauthorized state. The application exists and
     * is assigned to somebody else in the same role, and the officer gets AC-miniloan-127's sentence
     * rather than the row or a 404.
     */
    @Test
    void openingAnotherOfficersApplicationIsRefusedByTheApi() throws Exception {
        UUID theirs = submittedAndAssigned(ANOTHER_APPLICANT, ANOTHER_OFFICER);

        mockMvc
                .perform(get(APPLICATIONS + "/" + theirs).header("Authorization", OFFICER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_VISIBLE"))
                .andExpect(jsonPath("$.message").value("ไม่มีสิทธิ์เข้าถึงใบสมัครนี้"));
    }

    /** The other half — the assigned application really does open for the officer holding it. */
    @Test
    void theAssignedApplicationOpensForTheOfficerHoldingIt() throws Exception {
        UUID mine = submittedAndAssigned("ROLE-001", "ROLE-002");

        mockMvc
                .perform(get(APPLICATIONS + "/" + mine).header("Authorization", OFFICER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.id").value(mine.toString()));
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

    private UUID submittedAndAssigned(String applicantId, String loanOfficerId) {
        UUID id = draftFor(applicantId);
        submitService.submit(id, applicantId);
        assignmentService.assign(id, loanOfficerId, "ROLE-003");
        return id;
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

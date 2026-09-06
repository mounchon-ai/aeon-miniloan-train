package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miniloan.controller.RepaymentScheduleController.RescheduleResponse;
import com.miniloan.controller.RepaymentScheduleController.RevisionResponse;
import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.ApplicationAssignmentService;
import com.miniloan.service.DisbursementService;
import com.miniloan.service.LoanApplicationApprovalService;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationSubmitService;
import com.miniloan.service.RepaymentScheduleReissueService.AccountClosedException;
import com.miniloan.service.RepaymentScheduleReissueService.LoanAccountNotFoundException;
import com.miniloan.service.RepaymentScheduleReissueService.NotAssignedOperationsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * BR-miniloan-025@v1 at the layer it names, for UC-miniloan-010: a caller who never loads a screen
 * gets the same answer the screen would have given.
 *
 * <p>AC-miniloan-009 and AC-miniloan-010 each say "ทั้งจากหน้าจอและ API" in so many words, and the
 * API half of both is measured here — the route refuses, it says the sentence the criterion quotes,
 * and the data it was asked to change is still exactly as it was.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RepaymentScheduleControllerTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ADMIN = "ROLE-005";

    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    /** FE-miniloan-002's scheme — AuthTokenFilter gates every route, this one included. */
    private static final String OPERATIONS_TOKEN = "Bearer mock-role-004";

    @Autowired private MockMvc mockMvc;
    @Autowired private RepaymentScheduleController controller;
    @Autowired private DisbursementService disbursement;
    @Autowired private LoanApplicationApprovalService approvalService;
    @Autowired private ApplicationAssignmentService assignmentService;
    @Autowired private LoanApplicationSubmitService submitService;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private LoanApplicationRepository applications;
    @Autowired private LoanAccountRepository accounts;
    @Autowired private InterestRateVersionRepository rateVersions;
    @Autowired private RepaymentScheduleRepository schedules;
    @Autowired private InstallmentRepository installments;
    @Autowired private ApplicationAssignmentRepository assignments;
    @Autowired private CreditAssessmentRepository assessments;
    @Autowired private IdempotencyKeyRepository idempotencyKeys;
    @Autowired private PlatformTransactionManager transactionManager;

    @PersistenceContext private EntityManager entityManager;

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

    private static MockHttpServletRequest as(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE, role);
        return request;
    }

    private LoanAccount disbursedAccount() {
        rateVersions.save(
                new InterestRateVersion(RATE_25_PERCENT, LocalDate.now().minusYears(1), SUPERVISOR));
        UUID applicationId =
                draftService
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
        submitService.submit(applicationId, APPLICANT);
        assignmentService.assign(applicationId, OFFICER, SUPERVISOR);
        approvalService.approve(applicationId, null, OFFICER);
        return disbursement.disburse(applicationId, OFFICER).account();
    }

    /** See {@code AmortizationScheduleReissueTest}: FE-miniloan-013 owns closing, this only arranges it. */
    private void forceClosed(UUID accountId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                entityManager
                                        .createQuery("update LoanAccount a set a.status = :closed where a.id = :id")
                                        .setParameter("closed", LoanAccount.Status.Closed)
                                        .setParameter("id", accountId)
                                        .executeUpdate());
    }

    // ── API-011 · the route itself (AC-miniloan-008 · AC-miniloan-011) ─────────

    @Test
    void theAssignedOperationsPersonMayReissueThroughTheRouteAlone() {
        LoanAccount account = disbursedAccount();

        var response = controller.reschedule(account.getId(), as(OPERATIONS));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        RescheduleResponse body = response.getBody();
        assertThat(body.revisionNumber()).isEqualTo(2);
        assertThat(body.accountStatus()).isEqualTo("Active");
        assertThat(body.installments()).hasSize(12);
        assertThat(body.totalPrincipal()).isEqualByComparingTo("100000.00");

        // AC-miniloan-008: the reader can say which revision is in use and when the previous one was
        // replaced, from this response alone.
        assertThat(body.revisions()).hasSize(2);
        RevisionResponse previous = body.revisions().get(0);
        assertThat(previous.revisionNumber()).isEqualTo(1);
        assertThat(previous.current()).isFalse();
        assertThat(previous.supersededAt()).isEqualTo(body.issuedAt());
        assertThat(body.revisions().get(1).current()).isTrue();
        assertThat(body.revisions().get(1).supersededAt()).isNull();
    }

    /** AC-miniloan-011 through the route: revision 3, and the two before it still listed. */
    @Test
    void reissuingTwiceThroughTheRouteListsEveryRevision() {
        LoanAccount account = disbursedAccount();

        controller.reschedule(account.getId(), as(OPERATIONS));
        var response = controller.reschedule(account.getId(), as(OPERATIONS));

        assertThat(response.getBody().revisionNumber()).isEqualTo(3);
        assertThat(response.getBody().revisions())
                .extracting(RevisionResponse::revisionNumber)
                .containsExactly(1, 2, 3);
        assertThat(response.getBody().revisions()).filteredOn(RevisionResponse::current).hasSize(1);
    }

    // ── AC-miniloan-009 · a row of the revision in force cannot be edited ──────

    /**
     * AC-miniloan-009: the API refuses the row edit with the sentence the criterion quotes, and
     * instalment 7 still carries every value it had.
     */
    @Test
    void theApiRefusesToEditOneRowAndTheRowIsUnchanged() {
        LoanAccount account = disbursedAccount();
        var current = schedules.findByLoanAccountIdAndCurrentIsTrue(account.getId()).orElseThrow();
        List<Installment> before =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId());
        Installment seventhBefore = before.get(6);

        var response = controller.refuseRowEdit(account.getId(), 7);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("SCHEDULE_ROW_IMMUTABLE");
        assertThat(response.getBody().message())
                .isEqualTo("แก้ตารางผ่อนรายงวดไม่ได้ — ถ้าต้องเปลี่ยน ให้ออกตารางผ่อนฉบับใหม่ทับทั้งฉบับ");

        Installment seventhAfter =
                installments
                        .findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId())
                        .get(6);
        assertThat(seventhAfter.getEmiAmount()).isEqualByComparingTo(seventhBefore.getEmiAmount());
        assertThat(seventhAfter.getInterestPortion()).isEqualByComparingTo(seventhBefore.getInterestPortion());
        assertThat(seventhAfter.getPrincipalPortion()).isEqualByComparingTo(seventhBefore.getPrincipalPortion());
        assertThat(seventhAfter.getRemainingBalance()).isEqualByComparingTo(seventhBefore.getRemainingBalance());
        assertThat(seventhAfter.getDueDate()).isEqualTo(seventhBefore.getDueDate());
        assertThat(seventhAfter.getStatus()).isEqualTo(seventhBefore.getStatus());
    }

    // ── AC-miniloan-010 · a superseded revision cannot be deleted ──────────────

    /** AC-miniloan-010: revision 1 is refused deletion and stays readable with all twelve rows. */
    @Test
    void theApiRefusesToDeleteASupersededRevisionAndItStaysReadable() {
        LoanAccount account = disbursedAccount();
        controller.reschedule(account.getId(), as(OPERATIONS));

        var response = controller.refuseRevisionDelete(account.getId(), 1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("SUPERSEDED_SCHEDULE_RETAINED");
        assertThat(response.getBody().message())
                .isEqualTo("ลบตารางผ่อนฉบับเก่าไม่ได้ — ฉบับที่ถูกแทนที่ต้องเก็บไว้ให้ดูย้อนหลังได้");

        var first =
                schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId()).get(0);
        assertThat(first.getRevisionNumber()).isEqualTo(1);
        assertThat(installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(first.getId()))
                .hasSize(12);
    }

    // ── ACL-009 · who may call it at all ───────────────────────────────────────

    /** ACL-009 names ROLE-004 only; every other role is refused by the route before the service runs. */
    @Test
    void anyRoleOtherThanOperationsIsRefusedByTheRoute() {
        LoanAccount account = disbursedAccount();

        for (String role : List.of(APPLICANT, OFFICER, SUPERVISOR, ADMIN)) {
            assertThatThrownBy(() -> controller.reschedule(account.getId(), as(role)))
                    .isInstanceOf(RepaymentScheduleController.ForbiddenRoleException.class);
        }

        assertThat(controller.handleForbiddenRole().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.handleForbiddenRole().getBody().message())
                .isEqualTo("ไม่มีสิทธิ์ออกตารางผ่อนฉบับใหม่ทับ");
        assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId())).hasSize(1);
    }

    // ── the refusals the route maps to a status code ───────────────────────────

    /** AC-miniloan-107 through the route — BR-miniloan-045@v1 answers with 409, not with a 500. */
    @Test
    void aClosedAccountIsRefusedByTheRouteAsAConflict() {
        LoanAccount account = disbursedAccount();
        forceClosed(account.getId());

        assertThatThrownBy(() -> controller.reschedule(account.getId(), as(OPERATIONS)))
                .isInstanceOf(AccountClosedException.class);

        var mapped = controller.handleAccountClosed(new AccountClosedException());
        assertThat(mapped.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mapped.getBody().code()).isEqualTo("LOAN_ACCOUNT_CLOSED");
        assertThat(mapped.getBody().message()).isEqualTo("บัญชีนี้ปิดแล้ว — ออกตารางผ่อนฉบับใหม่ทับไม่ได้");
    }

    @Test
    void anUnknownAccountIsANotFound() {
        UUID missing = UUID.randomUUID();

        assertThatThrownBy(() -> controller.reschedule(missing, as(OPERATIONS)))
                .isInstanceOf(LoanAccountNotFoundException.class);

        assertThat(controller.handleNotFound(new LoanAccountNotFoundException(missing)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anotherOperationsPersonIsForbiddenByTheRoute() {
        assertThat(controller.handleNotAssigned(new NotAssignedOperationsException()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── the routes really are wired at those paths and verbs ──────────────────
    //
    // Everything above calls the controller bean. That measures the DECISION but not the ROUTING,
    // and routing is the whole point of the two refusal endpoints: a wrong path or verb in an
    // annotation puts a real PATCH or DELETE back on a 404 — the ABSENCE of a route, which is what
    // AC-miniloan-009 and AC-miniloan-010 must NOT be answered by (BR-miniloan-025@v1) — while every
    // direct-call assertion stays green. These three go over HTTP, through AuthTokenFilter, and are
    // the only proof that the paths in this file's annotations are the paths that exist.

    /** AC-miniloan-009 over HTTP: PATCH on a row of the revision in force is refused, not missing. */
    @Test
    void theRowEditRouteAnswersARealPatchWithTheRefusal() throws Exception {
        LoanAccount account = disbursedAccount();

        mockMvc
                .perform(
                        patch("/loan-accounts/{id}/installments/{number}", account.getId(), 7)
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_ROW_IMMUTABLE"))
                .andExpect(
                        jsonPath("$.message")
                                .value("แก้ตารางผ่อนรายงวดไม่ได้ — ถ้าต้องเปลี่ยน ให้ออกตารางผ่อนฉบับใหม่ทับทั้งฉบับ"));
    }

    /** AC-miniloan-010 over HTTP: DELETE on a superseded revision is refused, not missing. */
    @Test
    void theRevisionDeleteRouteAnswersARealDeleteWithTheRefusal() throws Exception {
        LoanAccount account = disbursedAccount();
        controller.reschedule(account.getId(), as(OPERATIONS));

        mockMvc
                .perform(
                        delete("/loan-accounts/{id}/schedules/{revision}", account.getId(), 1)
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPERSEDED_SCHEDULE_RETAINED"))
                .andExpect(
                        jsonPath("$.message")
                                .value("ลบตารางผ่อนฉบับเก่าไม่ได้ — ฉบับที่ถูกแทนที่ต้องเก็บไว้ให้ดูย้อนหลังได้"));

        assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId())).hasSize(2);
    }

    /** API-011 over HTTP — the operation interfaces.json declares, at the path it declares. */
    @Test
    void theRescheduleRouteAnswersARealPostAtTheDeclaredPath() throws Exception {
        LoanAccount account = disbursedAccount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/reschedule", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(2))
                .andExpect(jsonPath("$.installments.length()").value(12))
                .andExpect(jsonPath("$.revisions.length()").value(2));
    }
}

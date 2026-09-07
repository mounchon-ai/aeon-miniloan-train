package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miniloan.controller.RepaymentScheduleController.RescheduleResponse;
import com.miniloan.controller.RepaymentScheduleController.RevisionResponse;
import com.miniloan.controller.RepaymentScheduleController.ScheduleResponse;
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
import com.miniloan.service.LoanAccountScopeService;
import com.miniloan.service.LoanApplicationApprovalService;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationSubmitService;
import com.miniloan.service.RepaymentScheduleQueryService.AccountNotActiveException;
import com.miniloan.service.RepaymentScheduleQueryService.NotAccountOwnerException;
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

    private static final String APPLICANT_TOKEN = "Bearer mock-role-001";

    /**
     * AC-miniloan-099's "ผู้สมัคร ข." — a different PERSON, not a different role. The mock scheme
     * mints one token per role, and the identity the service compares is a plain String, so a second
     * applicant is expressible without touching {@code MockTokenService}.
     */
    private static final String ANOTHER_APPLICANT = "ROLE-001-another-person";

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
        return disbursedAccountOwnedBy(APPLICANT);
    }

    private LoanAccount disbursedAccountOwnedBy(String applicantId) {
        if (rateVersions.count() == 0) {
            rateVersions.save(
                    new InterestRateVersion(RATE_25_PERCENT, LocalDate.now().minusYears(1), SUPERVISOR));
        }
        UUID applicationId =
                draftService
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
        submitService.submit(applicationId, applicantId);
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

    /** Retires every row without going through FE-miniloan-012's service, which this unit does not own. */
    private void forceAllInstallmentsPaid(UUID accountId) {
        UUID scheduleId = schedules.findByLoanAccountIdAndCurrentIsTrue(accountId).orElseThrow().getId();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                entityManager
                                        .createQuery(
                                                "update Installment i set i.status = :paid where i.repaymentScheduleId = :sid")
                                        .setParameter("paid", Installment.Status.Paid)
                                        .setParameter("sid", scheduleId)
                                        .executeUpdate());
    }

    /** ACL-020's scope is a column, so a second Operations person is expressed by moving it. */
    private void forceAssignedOperations(UUID accountId, String operationsId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                entityManager
                                        .createQuery(
                                                "update LoanAccount a set a.assignedOperationsId = :who where a.id = :id")
                                        .setParameter("who", operationsId)
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

    // ── API-012 · ดูตารางผ่อนชำระ (FE-miniloan-012) ────────────────────────────
    //
    // AC-miniloan-099 says the refusal happens "ทั้งจากหน้าจอและด้วยการเรียก API ตรง", so the API
    // half is measured over HTTP, through AuthTokenFilter, at the path interfaces.json declares —
    // a bean call would measure the decision and leave a wrong path or verb green.

    /** AC-miniloan-098 over HTTP: the owner gets the whole table at the declared path. */
    @Test
    void theScheduleRouteAnswersARealGetWithTheWholeTable() throws Exception {
        LoanAccount account = disbursedAccount();

        mockMvc
                .perform(
                        get("/loan-accounts/{id}/schedule", account.getId())
                                .header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(1))
                .andExpect(jsonPath("$.accountStatus").value("Active"))
                .andExpect(jsonPath("$.installments.length()").value(12))
                .andExpect(jsonPath("$.installments[0].number").value(1))
                .andExpect(jsonPath("$.installments[11].number").value(12))
                .andExpect(jsonPath("$.installments[0].status").value("Due"))
                .andExpect(jsonPath("$.installments[11].remainingBalance").value(0));
    }

    /**
     * AC-miniloan-099 over HTTP — ก. calls the API directly for ข.'s account: 403, the sentence the
     * criterion quotes, and not one instalment row in the body.
     */
    @Test
    void callingTheApiDirectlyForAnotherApplicantsScheduleIsRefusedWithNoRowLeaked() throws Exception {
        LoanAccount theirs = disbursedAccountOwnedBy(ANOTHER_APPLICANT);

        mockMvc
                .perform(
                        get("/loan-accounts/{id}/schedule", theirs.getId())
                                .header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCHEDULE_VIEW_APPLICANT_ONLY"))
                .andExpect(jsonPath("$.message").value("ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้"))
                .andExpect(jsonPath("$.installments").doesNotExist());

        // ข.'s table is still whole — the refusal read nothing away.
        var current = schedules.findByLoanAccountIdAndCurrentIsTrue(theirs.getId()).orElseThrow();
        assertThat(installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId()))
                .hasSize(12);
    }

    /**
     * Two entries reach this route — ACL-010 (ROLE-001) and ACL-020 (ROLE-004) — and
     * {@code rbac.json} denies by default, so the other three roles are turned away before the
     * service runs, with the same sentence, so the refusal never says whether the account exists.
     *
     * <p><b>OPERATIONS was in this list until FE-miniloan-027 and dropping it is not a weakened
     * measure.</b> ACL-020 has declared {@code scope: own} with {@code enforceAt: [api, domain]}
     * since the permission matrix was written, and ACL-033 puts a {@code schedule-table} zone on
     * UI-miniloan-012; what this loop recorded about ROLE-004 was that the branch had not been
     * built, never that the rule denied it. The rule ACL-010 states — that a role which is not the
     * owning Applicant is refused the OWNER's read — is unchanged and is measured by the three roles
     * still here and by {@link #callingTheApiDirectlyForAnotherApplicantsScheduleIsRefusedWithNoRowLeaked}.
     * ROLE-004's own scope is measured below, and it is a narrower permission, not a wider one.
     */
    @Test
    void anyRoleOtherThanApplicantOrAssignedOperationsIsRefusedByTheScheduleRoute() {
        LoanAccount account = disbursedAccount();

        for (String role : List.of(OFFICER, SUPERVISOR, ADMIN)) {
            assertThatThrownBy(() -> controller.schedule(account.getId(), as(role)))
                    .isInstanceOf(NotAccountOwnerException.class);
        }

        assertThat(controller.handleNotAccountOwner().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.handleNotAccountOwner().getBody().message())
                .isEqualTo("ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้");
    }

    // ── ACL-020 · ACL-033 · the Operations read (FE-miniloan-027) ──────────────

    /**
     * UI-miniloan-012's {@code schedule-table} zone is four ENT-008 fields on a ROLE-004 screen, and
     * ACL-020 is the entry behind it. The whole current revision comes back — not the part still
     * ahead — exactly as it does for the owner.
     */
    @Test
    void theAssignedOperationsPersonReadsTheScheduleOfTheirOwnAccount() {
        LoanAccount account = disbursedAccount();

        var response = controller.schedule(account.getId(), as(OPERATIONS));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ScheduleResponse body = response.getBody();
        assertThat(body.revisionNumber()).isEqualTo(1);
        assertThat(body.installments()).hasSize(12);
        assertThat(body.installments().get(0).number()).isEqualTo(1);
        assertThat(body.installments().get(11).number()).isEqualTo(12);
        assertThat(body.principalAmount()).isEqualByComparingTo("100000.00");
    }

    /**
     * ACL-020's scope is {@code own}, and BR-miniloan-054@v1 is why the refusal reads the same as
     * "not there": an Operations person who could tell the two apart could enumerate the accounts
     * other people are assigned to.
     */
    @Test
    void anotherOperationsPersonIsRefusedTheScheduleWithTheSameSentence() {
        LoanAccount account = disbursedAccount();
        forceAssignedOperations(account.getId(), "ROLE-004-another-person");

        assertThatThrownBy(() -> controller.schedule(account.getId(), as(OPERATIONS)))
                .isInstanceOf(LoanAccountScopeService.NotVisibleException.class)
                .hasMessage("ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้");
    }

    /**
     * ACL-010 declares {@code condition: Active} and ACL-020 declares NO condition, and that
     * difference is deliberate rather than an oversight this route should smooth over: ACL-015 files
     * an adjustment against a CLOSED account from UI-miniloan-012, which is the same screen the
     * schedule zone sits on. An Operations person locked out of a closed account's table could not
     * use the screen AC-miniloan-076 is about. The owner's Active precondition is untouched —
     * {@link #aClosedAccountIsRefusedByTheScheduleRouteAsAConflict} still measures it.
     */
    @Test
    void aClosedAccountIsStillReadableByTheAssignedOperationsPerson() {
        LoanAccount account = disbursedAccount();
        forceClosed(account.getId());

        var response = controller.schedule(account.getId(), as(OPERATIONS));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accountStatus()).isEqualTo("Closed");
        assertThat(response.getBody().installments()).hasSize(12);

        // and the owner is still refused the same account, by ACL-010's own condition
        assertThatThrownBy(() -> controller.schedule(account.getId(), as(APPLICANT)))
                .isInstanceOf(AccountNotActiveException.class);
    }

    /** Over HTTP, with FE-miniloan-002's token, which is how UI-miniloan-012 will actually ask. */
    @Test
    void theScheduleRouteAnswersARealGetForTheAssignedOperationsPerson() throws Exception {
        LoanAccount account = disbursedAccount();

        mockMvc
                .perform(
                        get("/loan-accounts/{id}/schedule", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(1))
                .andExpect(jsonPath("$.installments.length()").value(12));
    }

    /** The route's own reading of the same table the reschedule route hands back. */
    @Test
    void theOwnerReadsTheRevisionInForceThroughTheRoute() {
        LoanAccount account = disbursedAccount();
        controller.reschedule(account.getId(), as(OPERATIONS));

        var response = controller.schedule(account.getId(), as(APPLICANT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ScheduleResponse body = response.getBody();
        assertThat(body.revisionNumber()).isEqualTo(2);
        assertThat(body.installments()).hasSize(12);
        assertThat(body.totalPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(body.principalAmount()).isEqualByComparingTo("100000.00");
        assertThat(body.termMonths()).isEqualTo(12);
    }

    /** ACL-010's condition, mapped by the route: a Closed account is a 409, not a 500. */
    @Test
    void aClosedAccountIsRefusedByTheScheduleRouteAsAConflict() {
        LoanAccount account = disbursedAccount();
        forceClosed(account.getId());

        assertThatThrownBy(() -> controller.schedule(account.getId(), as(APPLICANT)))
                .isInstanceOf(AccountNotActiveException.class);

        var mapped = controller.handleAccountNotActive(new AccountNotActiveException());
        assertThat(mapped.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mapped.getBody().code()).isEqualTo("LOAN_ACCOUNT_NOT_ACTIVE");
        assertThat(mapped.getBody().message()).isEqualTo("ดูตารางผ่อนไม่ได้ — บัญชีนี้ปิดแล้ว");
    }

    // ── UI-miniloan-011's next-due-installment (API-013 · API-023) ─────────────

    /**
     * The field mock named in conventions.json fieldMap[], answered for a whole page in one query.
     * A freshly disbursed account owes instalment 1, and both routes say so — the list and the
     * single read are the same record, so a row and the detail page it opens cannot disagree.
     */
    @Test
    void theAccountListCarriesTheNextInstalmentStillOwing() throws Exception {
        LoanAccount account = disbursedAccount();

        mockMvc
                .perform(get("/loan-accounts").header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nextDueInstallmentNumber").value(1));

        mockMvc
                .perform(
                        get("/loan-accounts/{id}", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextDueInstallmentNumber").value(1));
    }

    /**
     * Nothing left owing is NULL and not zero. Zero would read as instalment number zero, and a
     * screen that printed it would tell an Operations person a row exists that does not; the
     * criterion this protects is the ordinary one, that the field names a real instalment or none.
     */
    @Test
    void anAccountWithNothingOwingCarriesNoNextInstalment() throws Exception {
        LoanAccount account = disbursedAccount();
        forceAllInstallmentsPaid(account.getId());

        mockMvc
                .perform(
                        get("/loan-accounts/{id}", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextDueInstallmentNumber").doesNotExist());
    }

    /**
     * BR-miniloan-020@v1 retires instalments in order, so "the next Due" is the LOWEST-numbered Due
     * row. A query that returned any Due row would pass a fixture where only row 1 is outstanding
     * and fail here.
     */
    @Test
    void theNextInstalmentIsTheLowestNumberedDueRow() throws Exception {
        LoanAccount account = disbursedAccount();
        forceAllInstallmentsPaid(account.getId());
        forceInstallmentsDueFrom(account.getId(), 4);

        mockMvc
                .perform(
                        get("/loan-accounts/{id}", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextDueInstallmentNumber").value(4));
    }

    private void forceInstallmentsDueFrom(UUID accountId, int from) {
        UUID scheduleId = schedules.findByLoanAccountIdAndCurrentIsTrue(accountId).orElseThrow().getId();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                entityManager
                                        .createQuery(
                                                "update Installment i set i.status = :due"
                                                        + " where i.repaymentScheduleId = :sid and i.installmentNumber >= :from")
                                        .setParameter("due", Installment.Status.Due)
                                        .setParameter("sid", scheduleId)
                                        .setParameter("from", from)
                                        .executeUpdate());
    }
}

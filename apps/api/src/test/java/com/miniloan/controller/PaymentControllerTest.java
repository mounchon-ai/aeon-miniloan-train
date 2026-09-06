package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.miniloan.repository.PaymentRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.ApplicationAssignmentService;
import com.miniloan.service.DisbursementService;
import com.miniloan.service.LoanApplicationApprovalService;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationSubmitService;
import com.miniloan.service.PaymentService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * BR-miniloan-025@v1 at the layer it names, for UC-miniloan-012 · UC-miniloan-014 · UC-miniloan-024:
 * a caller who never loads a screen gets exactly the answer the screen would have given.
 *
 * <p>Three criteria say so in as many words and are measured here over real HTTP, through
 * {@link AuthTokenFilter}, at the paths the annotations declare — AC-miniloan-070 ("ด้วยการเรียก API
 * บันทึกการชำระโดยตรง"), AC-miniloan-007 ("ด้วยการเรียก API ปิดบัญชีด้วย id เดิมโดยตรง") and
 * AC-miniloan-136 ("ด้วย id ของบัญชี Y โดยตรง ไม่ผ่านหน้าจอ"). {@code PaymentServiceTest} measures
 * the same decisions at the domain layer; what only these can prove is that the routes exist at all
 * — a wrong path or verb puts a real POST back on a 404, which is the ABSENCE of a route rather than
 * the enforcement of a rule.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ANOTHER_OPERATIONS = "ROLE-004-another-person";

    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    /** FE-miniloan-002's scheme — AuthTokenFilter gates every route, these included. */
    private static final String OPERATIONS_TOKEN = "Bearer mock-role-004";

    private static final String APPLICANT_TOKEN = "Bearer mock-role-001";

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentService paymentService;
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
    @Autowired private PaymentRepository payments;
    @Autowired private ApplicationAssignmentRepository assignments;
    @Autowired private CreditAssessmentRepository assessments;
    @Autowired private IdempotencyKeyRepository idempotencyKeys;
    @Autowired private PlatformTransactionManager transactionManager;

    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void clean() {
        payments.deleteAll();
        installments.deleteAll();
        schedules.deleteAll();
        accounts.deleteAll();
        assignments.deleteAll();
        assessments.deleteAll();
        idempotencyKeys.deleteAll();
        applications.deleteAll();
        rateVersions.deleteAll();
    }

    private LoanAccount disbursedAccount() {
        if (rateVersions.count() == 0) {
            rateVersions.save(
                    new InterestRateVersion(RATE_25_PERCENT, LocalDate.now().minusYears(1), SUPERVISOR));
        }
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

    private void reassignTo(UUID accountId, String operationsId) {
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

    private List<Installment> currentRows(UUID accountId) {
        var current = schedules.findByLoanAccountIdAndCurrentIsTrue(accountId).orElseThrow();
        return installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId());
    }

    private String body(int installmentNumber, BigDecimal amount) {
        return "{\"installmentNumber\":" + installmentNumber + ",\"amount\":" + amount.toPlainString() + "}";
    }

    // ── API-014 · the route itself ───────────────────────────────────────────

    /** AC-miniloan-013 over HTTP, at the operation interfaces.json declares. */
    @Test
    void thePaymentRouteAnswersARealPostAtTheDeclaredPath() throws Exception {
        LoanAccount account = disbursedAccount();
        BigDecimal owed = currentRows(account.getId()).get(0).getEmiAmount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/payments", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(1, owed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentNumber").value(1))
                .andExpect(jsonPath("$.installmentStatus").value("Paid"))
                .andExpect(jsonPath("$.paymentType").value("InstallmentExact"))
                .andExpect(jsonPath("$.accountStatus").value("Active"));
    }

    /** AC-miniloan-086 over HTTP: the figures the screen renders its sentence from. */
    @Test
    void theOverpaymentFiguresTravelBackOverTheRoute() throws Exception {
        LoanAccount account = disbursedAccount();
        for (int number = 1; number <= 5; number++) {
            paymentService.record(
                    account.getId(), OPERATIONS, number, currentRows(account.getId()).get(number - 1).getEmiAmount(), null);
        }
        BigDecimal owed = currentRows(account.getId()).get(5).getEmiAmount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/payments", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(6, owed.add(new BigDecimal("20000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentType").value("InstallmentOverpayment"))
                .andExpect(jsonPath("$.overpaymentAmount").value(20000.00))
                .andExpect(jsonPath("$.prepaymentFee").value(200.00))
                .andExpect(jsonPath("$.reissuedRevisionNumber").value(2));
    }

    /**
     * AC-miniloan-070 over HTTP: the Applicant owns this account, holds a valid token, calls the API
     * directly — and is refused with the sentence the criterion quotes. The instalment does not move.
     */
    @Test
    void anApplicantCallingThePaymentApiDirectlyIsRefused() throws Exception {
        LoanAccount account = disbursedAccount();
        BigDecimal owed = currentRows(account.getId()).get(0).getEmiAmount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/payments", account.getId())
                                .header("Authorization", APPLICANT_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(1, owed)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAYMENT_OPERATIONS_ONLY"))
                .andExpect(
                        jsonPath("$.message")
                                .value("ไม่มีสิทธิ์บันทึกการชำระ — การบันทึกการชำระทำได้เฉพาะเจ้าหน้าที่ Operations"));

        assertThat(currentRows(account.getId()).get(0).getStatus()).isEqualTo(Installment.Status.Due);
        assertThat(payments.findByLoanAccountIdOrderByRecordedAtAsc(account.getId())).isEmpty();
    }

    /** AC-miniloan-012 over HTTP: short by one satang, refused, and nothing changes. */
    @Test
    void aShortPaymentIsRefusedOverTheRouteToo() throws Exception {
        LoanAccount account = disbursedAccount();
        BigDecimal owed = currentRows(account.getId()).get(0).getEmiAmount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/payments", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(1, owed.subtract(new BigDecimal("0.01")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTIAL_PAYMENT_REFUSED"));

        assertThat(currentRows(account.getId()).get(0).getStatus()).isEqualTo(Installment.Status.Due);
    }

    // ── the close route that answers to no API- id ───────────────────────────

    /**
     * AC-miniloan-006 over HTTP: the close command is a real route that REFUSES, not a path that is
     * missing. A 404 here would be indistinguishable from a typo.
     */
    @Test
    void theCloseRouteRefusesARealPostRatherThanBeingAbsent() throws Exception {
        LoanAccount account = disbursedAccount();
        for (int number = 1; number <= 5; number++) {
            paymentService.record(
                    account.getId(), OPERATIONS, number, currentRows(account.getId()).get(number - 1).getEmiAmount(), null);
        }

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/close", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_CLOSE_NOT_A_COMMAND"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "ปิดบัญชีไม่ได้ — บัญชีนี้ยังมีงวดค้าง 7 งวด · ปิดบัญชีได้เมื่อชำระครบทุกงวด"
                                                + " หรือชำระยอดปิดบัญชีก่อนกำหนดครบเท่านั้น"));

        assertThat(accounts.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanAccount.Status.Active);
    }

    /** AC-miniloan-007 over HTTP: closing a closed account again is refused, status unchanged. */
    @Test
    void closingAnAlreadyClosedAccountOverTheRouteIsRefused() throws Exception {
        LoanAccount account = disbursedAccount();
        for (int number = 1; number <= 12; number++) {
            paymentService.record(
                    account.getId(), OPERATIONS, number, currentRows(account.getId()).get(number - 1).getEmiAmount(), null);
        }

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/close", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_ACCOUNT_ALREADY_CLOSED"))
                .andExpect(jsonPath("$.message").value("บัญชีนี้ปิดแล้ว — ปิดซ้ำไม่ได้"));

        assertThat(accounts.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanAccount.Status.Closed);
    }

    /** BR-miniloan-034@v1's closing half over HTTP — its own sentence, not the payment one. */
    @Test
    void anApplicantCallingTheCloseApiDirectlyIsRefusedWithItsOwnSentence() throws Exception {
        LoanAccount account = disbursedAccount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/close", account.getId())
                                .header("Authorization", APPLICANT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLOSE_OPERATIONS_ONLY"))
                .andExpect(
                        jsonPath("$.message")
                                .value("ไม่มีสิทธิ์ปิดบัญชีสินเชื่อ — การปิดบัญชีทำได้เฉพาะเจ้าหน้าที่ Operations"));

        assertThat(accounts.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanAccount.Status.Active);
    }

    /** AC-miniloan-136 over HTTP: both commands, one refusal, account Y untouched. */
    @Test
    void anUnassignedOperationsIsRefusedBothRoutesOverHttp() throws Exception {
        LoanAccount theirs = disbursedAccount();
        reassignTo(theirs.getId(), ANOTHER_OPERATIONS);
        BigDecimal owed = currentRows(theirs.getId()).get(0).getEmiAmount();
        BigDecimal before = accounts.findById(theirs.getId()).orElseThrow().getOutstandingPrincipal();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/payments", theirs.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(1, owed)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ASSIGNED_TO_ANOTHER"))
                .andExpect(
                        jsonPath("$.message")
                                .value("ไม่มีสิทธิ์ดำเนินการกับบัญชีสินเชื่อนี้ — ไม่ได้ถูก assign ดูแลบัญชีนี้"));

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/close", theirs.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ASSIGNED_TO_ANOTHER"));

        LoanAccount untouched = accounts.findById(theirs.getId()).orElseThrow();
        assertThat(untouched.getOutstandingPrincipal()).isEqualByComparingTo(before);
        assertThat(untouched.getStatus()).isEqualTo(LoanAccount.Status.Active);
        assertThat(currentRows(theirs.getId()).get(0).getStatus()).isEqualTo(Installment.Status.Due);
    }

    // ── API-013 · API-023 · the scoped list over HTTP (AC-miniloan-137) ───────

    /**
     * AC-miniloan-137 over HTTP. The count is asserted, not merely the presence of the caller's own
     * account: "ไม่มีบัญชีของ Operations คนอื่นหลุดเข้ามาแม้แต่บัญชีเดียว".
     */
    @Test
    void theAccountListRouteReturnsOnlyTheCallersOwnAccounts() throws Exception {
        LoanAccount mine = disbursedAccount();
        LoanAccount theirs = disbursedAccount();
        reassignTo(theirs.getId(), ANOTHER_OPERATIONS);

        mockMvc
                .perform(get("/loan-accounts").header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountNumber").value(mine.getId().toString()));

        assertThat(accounts.count()).isEqualTo(2);
    }

    /** API-023 over HTTP, and the same scope answer as the list gives. */
    @Test
    void theAccountDetailRouteIsScopedTheSameWayTheListIs() throws Exception {
        LoanAccount mine = disbursedAccount();
        LoanAccount theirs = disbursedAccount();
        reassignTo(theirs.getId(), ANOTHER_OPERATIONS);

        mockMvc
                .perform(
                        get("/loan-accounts/{id}", mine.getId()).header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(mine.getId().toString()))
                .andExpect(jsonPath("$.status").value("Active"))
                .andExpect(jsonPath("$.principalAmount").value(100000.00));

        mockMvc
                .perform(
                        get("/loan-accounts/{id}", theirs.getId()).header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LOAN_ACCOUNT_NOT_VISIBLE"));
    }
}

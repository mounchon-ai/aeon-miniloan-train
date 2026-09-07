package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.DashboardSummaryService.DashboardNotPermittedException;
import com.miniloan.service.DashboardSummaryService.DashboardSummary;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * แดชบอร์ดภาพรวมสถานะ (UC-miniloan-020 · BR-miniloan-024@v1) — AC-miniloan-110 · AC-miniloan-111 ·
 * AC-miniloan-112, at the domain layer ACL-018 names alongside the API.
 *
 * <p><b>Nothing is computed here and no golden row is quoted.</b> BR-miniloan-024@v1 is a policy, not
 * a calculation: it has no CALC- contract and no signed dataset, so what is measured is that the
 * counts match the rows this test actually put on disk — built through the real services, never
 * written straight into the tables.
 *
 * <p><b>The states are reached the way the state machine allows, not by setting a column.</b> A
 * durably-Submitted application is one the automatic assessment left there — Band C does not move to
 * UnderReview, and that is the only path STM-miniloan-001 offers to a stored Submitted row. Closed is
 * the single exception: {@link LoanAccount} exposes no status mutator and FE-miniloan-013 is the unit
 * that closes an account, so the state is written through JPQL — the same arrangement {@code
 * RepaymentScheduleQueryServiceTest} and {@code AmortizationScheduleReissueTest} use.
 *
 * <p><b>Why AC-miniloan-110 numbers are system-wide.</b> ACL-018 says {@code scope: own} and
 * DQ-miniloan-004 answered the same, but neither is reachable: {@link LoanApplication#assignTo}
 * refuses anything but UnderReview, so a Submitted application has no officer to belong to, and
 * rbac.json gives ROLE-002 no scope over a loan account at all. Under strict {@code own} this
 * criterion could only read 0 · 2 · 1 · 0 · 0, and making it pass would mean editing the criterion.
 * The reasoning is set out in full on {@link DashboardSummaryService}; a card is open with design.
 */
@SpringBootTest
class DashboardSummaryServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ADJUSTMENT_APPROVER = "ROLE-005";

    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    /** Band A — 1,000 บาท of existing debt leaves the DTI far under 50%, so the assessment moves it on. */
    private static final BigDecimal DEBT_THAT_PASSES = new BigDecimal("1000.00");

    /**
     * Band C — 15,000 บาท of existing debt plus the ~9,497 บาท instalment is 24,497 against a 21,000
     * บาท ceiling (income × 0.70), so BR-miniloan-006@v1 stops the application at Submitted.
     */
    private static final BigDecimal DEBT_THAT_FAILS_DTI = new BigDecimal("15000.00");

    @Autowired private DashboardSummaryService dashboard;
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

    // ── AC-miniloan-110 · five numbers, two aggregates ────────────────────────

    /**
     * "ยื่นแล้ว 3 · อยู่ระหว่างพิจารณา 2 · อนุมัติแล้ว 1 · ใช้งานอยู่ 5 · ปิดแล้ว 4" — the given is
     * built row by row through the real flow, so the fifteen applications behind these numbers really
     * hold the statuses the criterion names.
     */
    @Test
    void theDashboardShowsAllFiveCountsAcrossBothAggregates() {
        for (int i = 0; i < 3; i++) {
            submittedApplication();
        }
        for (int i = 0; i < 2; i++) {
            underReviewApplication();
        }
        approvedApplication();
        for (int i = 0; i < 5; i++) {
            activeAccount();
        }
        for (int i = 0; i < 4; i++) {
            closedAccount();
        }

        DashboardSummary summary = dashboard.summaryFor(OFFICER);

        assertThat(summary.submittedApplications()).isEqualTo(3);
        assertThat(summary.underReviewApplications()).isEqualTo(2);
        assertThat(summary.approvedApplications()).isEqualTo(1);
        assertThat(summary.activeLoanAccounts()).isEqualTo(5);
        assertThat(summary.closedLoanAccounts()).isEqualTo(4);
    }

    /**
     * "สามตัวแรกนับจากใบสมัคร สองตัวหลังนับจากบัญชีสินเชื่อ" — the two aggregates are counted
     * independently, so a change on one side moves only its own squares. Disbursing an approved
     * application empties "อนุมัติแล้ว" and fills "ใช้งานอยู่", and touches nothing else.
     */
    @Test
    void theApplicationSquaresAndTheAccountSquaresMoveIndependently() {
        ensureRateVersion();
        UUID applicationId = approvedApplication();

        DashboardSummary before = dashboard.summaryFor(OFFICER);
        assertThat(before.approvedApplications()).isEqualTo(1);
        assertThat(before.activeLoanAccounts()).isZero();

        disbursement.disburse(applicationId, OFFICER);

        DashboardSummary after = dashboard.summaryFor(OFFICER);
        assertThat(after.approvedApplications()).isZero();
        assertThat(after.activeLoanAccounts()).isEqualTo(1);
        assertThat(after.submittedApplications()).isZero();
        assertThat(after.underReviewApplications()).isZero();
        assertThat(after.closedLoanAccounts()).isZero();
    }

    // ── AC-miniloan-111 · the lower bound of counting ─────────────────────────

    /**
     * "เป็นเลขศูนย์ ไม่ใช่ช่องว่าง ไม่ใช่ขีด และไม่ใช่ข้อความว่าไม่มีข้อมูล" — an empty system answers
     * with five zeros and no exception. The record fields are primitive {@code long}s, so there is no
     * null for a screen to render as a blank; this test measures that the read itself does not fail
     * and that every square is present.
     */
    @Test
    void anEmptySystemAnswersWithFiveZerosAndNoError() {
        assertThat(applications.count()).isZero();
        assertThat(accounts.count()).isZero();

        DashboardSummary summary = dashboard.summaryFor(OFFICER);

        assertThat(summary).isEqualTo(new DashboardSummary(0L, 0L, 0L, 0L, 0L));
    }

    // ── AC-miniloan-112 · counted once, across two aggregates ─────────────────

    /**
     * "ถูกนับเป็น ใช้งานอยู่ 1 เท่านั้น … ต้องไม่ถูกนับซ้ำในช่อง อนุมัติแล้ว ด้วย" — one application
     * that went all the way to Disbursed and the one account it created. Disbursed matches no square,
     * so the application contributes nothing and the account contributes exactly one.
     */
    @Test
    void aDisbursedApplicationIsCountedOnceAsAnActiveAccountAndNeverAlsoAsApproved() {
        LoanAccount account = activeAccount();

        assertThat(applications.count()).isEqualTo(1);
        assertThat(applications.findById(account.getApplicationId()).orElseThrow().getStatus())
                .isEqualTo(LoanApplication.Status.Disbursed);

        DashboardSummary summary = dashboard.summaryFor(OFFICER);

        assertThat(summary.activeLoanAccounts()).isEqualTo(1);
        assertThat(summary.approvedApplications()).isZero();
        assertThat(summary.submittedApplications()).isZero();
        assertThat(summary.underReviewApplications()).isZero();
        assertThat(summary.closedLoanAccounts()).isZero();
    }

    // ── ACL-018 · default-deny is the answer, not five zeros ──────────────────

    /**
     * rbac.json defaultEffect is deny and ACL-018 is the only entry naming UC-miniloan-020. A role
     * with no entry is refused rather than shown an empty dashboard — handing back zeros would be
     * answering a question the caller may not ask, and would read to them as "the system is empty".
     */
    @Test
    void aRoleWithNoEntryForThisUseCaseIsRefusedRatherThanShownZeros() {
        activeAccount();

        for (String role : new String[] {APPLICANT, SUPERVISOR, OPERATIONS, ADJUSTMENT_APPROVER}) {
            assertThatThrownBy(() -> dashboard.summaryFor(role))
                    .isInstanceOf(DashboardNotPermittedException.class);
        }
    }

    // ── arrangement ──────────────────────────────────────────────────────────

    private UUID draftWith(BigDecimal existingMonthlyDebt) {
        return draftService
                .saveNewDraft(
                        APPLICANT,
                        new DraftFields(
                                "ทดสอบ ผู้สมัคร",
                                35,
                                new BigDecimal("30000.00"),
                                24,
                                existingMonthlyDebt,
                                new BigDecimal("100000.00"),
                                12))
                .getId();
    }

    /** Band C: the assessment records it and leaves the application at Submitted. */
    private UUID submittedApplication() {
        UUID id = draftWith(DEBT_THAT_FAILS_DTI);
        submitService.submit(id, APPLICANT);
        return id;
    }

    /** Band A: the assessment moves it on by itself — no human presses anything. */
    private UUID underReviewApplication() {
        UUID id = draftWith(DEBT_THAT_PASSES);
        submitService.submit(id, APPLICANT);
        return id;
    }

    private UUID approvedApplication() {
        UUID id = underReviewApplication();
        assignmentService.assign(id, OFFICER, SUPERVISOR);
        approvalService.approve(id, null, OFFICER);
        return id;
    }

    private LoanAccount activeAccount() {
        ensureRateVersion();
        return disbursement.disburse(approvedApplication(), OFFICER).account();
    }

    private LoanAccount closedAccount() {
        LoanAccount account = activeAccount();
        forceClosed(account.getId());
        return account;
    }

    private void ensureRateVersion() {
        if (rateVersions.count() == 0) {
            rateVersions.save(
                    new InterestRateVersion(RATE_25_PERCENT, LocalDate.now().minusYears(1), SUPERVISOR));
        }
    }

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
}

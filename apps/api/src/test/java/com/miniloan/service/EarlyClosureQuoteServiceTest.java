package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.domain.EarlySettlementCalculator;
import com.miniloan.domain.EarlySettlementCalculator.ClosingDateBeforeLastPaidDueDateException;
import com.miniloan.domain.EarlySettlementCalculator.Payoff;
import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.Money;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.PaymentRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FE-miniloan-014 · UC-miniloan-015 · UC-miniloan-016.
 *
 * <p>{@link GoldenRows} is CALC-miniloan-004@v1 measured against GD-miniloan-005 — <b>every expected
 * value there is that dataset's row, copied character for character</b> (RQ24 · DV12). The dataset
 * was computed on 2026-09-01 and verified by aplus191 with {@code mismatches: []}; nothing here
 * recomputes it, because arithmetic dev did itself is dev grading its own homework.
 *
 * <p>The six rows the dataset marks {@code computed: true} exercise the calculator. Row 4 is marked
 * {@code computed: false} with {@code declined_by: "CALC-miniloan-004@v1 boundary_behavior"} — it is
 * the contract refusing a backdated closing date before the formula is reached, so it is asserted as
 * a refusal rather than as a number.
 *
 * <p>The rest of the class is the two services around that calculator: who may ask (ACL-013), who may
 * settle (ACL-014), and what one accepted payoff does to the account and to the instalments still Due
 * (AC-miniloan-005 · BR-miniloan-021@v1 · BR-miniloan-023@v1).
 */
@SpringBootTest
class EarlyClosureQuoteServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ANOTHER_OPERATIONS = "ROLE-004-another-person";

    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    @Autowired private EarlyClosureQuoteService quotes;
    @Autowired private EarlyClosureSettlementService settlements;
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
    @Autowired private Clock clock;

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

    /** The date the calculator will measure a zero-day span from, for an untouched account. */
    private LocalDate disbursementDay(LoanAccount account) {
        return LocalDate.ofInstant(account.getDisbursedAt(), clock.getZone());
    }

    // ── GD-miniloan-005, row by row ──────────────────────────────────────────

    @Nested
    @DisplayName("GD-miniloan-005 · CALC-miniloan-004@v1")
    class GoldenRows {

        private static final BigDecimal RATE_25 = new BigDecimal("0.25");
        private static final BigDecimal RATE_18 = new BigDecimal("0.18");
        private static final BigDecimal RATE_0 = new BigDecimal("0");

        /** Row 1 — 50,000.00 · 0.25 · 15 มิ.ย. → 25 มิ.ย. (AC-miniloan-073). */
        @Test
        void tenDaysAtTwentyFivePercent() {
            Payoff payoff =
                    EarlySettlementCalculator.quote(
                            new BigDecimal("50000.00"),
                            RATE_25,
                            LocalDate.parse("2026-06-15"),
                            LocalDate.parse("2026-06-25"));

            assertThat(payoff.daysElapsed()).isEqualTo(10);
            assertThat(Money.exact(payoff.remainingPrincipal())).isEqualTo("50,000.00");
            assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("342.47");
            assertThat(Money.exact(payoff.earlySettlementFee())).isEqualTo("500.00");
            assertThat(Money.exact(payoff.earlySettlementAmount())).isEqualTo("50,842.47");
        }

        /** Row 2 — a span of 0 days is 0.00 by definition, not by rounding (AC-miniloan-074). */
        @Test
        void aZeroDaySpanAccruesNothing() {
            Payoff payoff =
                    EarlySettlementCalculator.quote(
                            new BigDecimal("50000.00"),
                            RATE_25,
                            LocalDate.parse("2026-06-15"),
                            LocalDate.parse("2026-06-15"));

            assertThat(payoff.daysElapsed()).isZero();
            assertThat(Money.exact(payoff.remainingPrincipal())).isEqualTo("50,000.00");
            assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("0.00");
            assertThat(Money.exact(payoff.earlySettlementFee())).isEqualTo("500.00");
            assertThat(Money.exact(payoff.earlySettlementAmount())).isEqualTo("50,500.00");
        }

        /** Row 3 — one day is one day: /365, never a whole month or a whole instalment (AC-075). */
        @Test
        void oneDayIsOneDay() {
            Payoff payoff =
                    EarlySettlementCalculator.quote(
                            new BigDecimal("50000.00"),
                            RATE_25,
                            LocalDate.parse("2026-06-15"),
                            LocalDate.parse("2026-06-16"));

            assertThat(payoff.daysElapsed()).isEqualTo(1);
            assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("34.25");
            assertThat(Money.exact(payoff.earlySettlementAmount())).isEqualTo("50,534.25");
        }

        /**
         * Row 4 — {@code computed: false}, {@code declined_by: "CALC-miniloan-004@v1
         * boundary_behavior"}. The dataset has no numbers for this row because the contract refuses
         * the input before the formula, and a negative accrued interest is not an answer.
         */
        @Test
        void aBackdatedClosingDateIsRefusedBeforeTheFormula() {
            assertThatThrownBy(
                            () ->
                                    EarlySettlementCalculator.quote(
                                            new BigDecimal("50000.00"),
                                            RATE_25,
                                            LocalDate.parse("2026-06-15"),
                                            LocalDate.parse("2026-06-14")))
                    .isInstanceOf(ClosingDateBeforeLastPaidDueDateException.class)
                    .hasMessageContaining("2026-06-14")
                    .hasMessageContaining("2026-06-15");
        }

        /** Row 5 — 0%/yr accrues nothing over 30 days; the rate is in the numerator. */
        @Test
        void aZeroRateAccruesNothingOverAnySpan() {
            Payoff payoff =
                    EarlySettlementCalculator.quote(
                            new BigDecimal("50000.00"),
                            RATE_0,
                            LocalDate.parse("2026-06-15"),
                            LocalDate.parse("2026-07-15"));

            assertThat(payoff.daysElapsed()).isEqualTo(30);
            assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("0.00");
            assertThat(Money.exact(payoff.earlySettlementFee())).isEqualTo("500.00");
            assertThat(Money.exact(payoff.earlySettlementAmount())).isEqualTo("50,500.00");
        }

        /** Row 6 — 100,000.00 · 0.25 · 10 วัน. */
        @Test
        void oneHundredThousandOverTenDays() {
            Payoff payoff =
                    EarlySettlementCalculator.quote(
                            new BigDecimal("100000.00"),
                            RATE_25,
                            LocalDate.parse("2026-01-10"),
                            LocalDate.parse("2026-01-20"));

            assertThat(payoff.daysElapsed()).isEqualTo(10);
            assertThat(Money.exact(payoff.remainingPrincipal())).isEqualTo("100,000.00");
            assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("684.93");
            assertThat(Money.exact(payoff.earlySettlementFee())).isEqualTo("1,000.00");
            assertThat(Money.exact(payoff.earlySettlementAmount())).isEqualTo("101,684.93");
        }

        /** Row 7 — the row where both rounded lines carry a satang: 73,456.78 · 0.18 · 47 วัน. */
        @Test
        void anAwkwardPrincipalOverFortySevenDays() {
            Payoff payoff =
                    EarlySettlementCalculator.quote(
                            new BigDecimal("73456.78"),
                            RATE_18,
                            LocalDate.parse("2026-03-01"),
                            LocalDate.parse("2026-04-17"));

            assertThat(payoff.daysElapsed()).isEqualTo(47);
            assertThat(Money.exact(payoff.remainingPrincipal())).isEqualTo("73,456.78");
            assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("1,702.59");
            assertThat(Money.exact(payoff.earlySettlementFee())).isEqualTo("734.57");
            assertThat(Money.exact(payoff.earlySettlementAmount())).isEqualTo("75,893.94");
        }
    }

    // ── ACL-013 · who may ask for the figure ─────────────────────────────────

    /** AC-miniloan-071: the Applicant sees the figure, and nothing about the account moves. */
    @Test
    void theOwnerSeesTheFigureAndTheAccountDoesNotMove() {
        LoanAccount account = disbursedAccount();

        Payoff payoff = quotes.quoteFor(account.getId(), APPLICANT, disbursementDay(account));

        assertThat(payoff.daysElapsed()).isZero();
        assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("0.00");
        assertThat(Money.exact(payoff.remainingPrincipal())).isEqualTo("100,000.00");
        assertThat(Money.exact(payoff.earlySettlementFee())).isEqualTo("1,000.00");
        assertThat(Money.exact(payoff.earlySettlementAmount())).isEqualTo("101,000.00");

        LoanAccount after = accounts.findById(account.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(LoanAccount.Status.Active);
        assertThat(payments.count()).isZero();
        assertThat(currentRows(account.getId()))
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(Installment.Status.Due));
    }

    /**
     * ACL-013 names ROLE-001 alone and rbac.json is default-deny. AC-miniloan-073/074/075 write
     * "Operations เปิดหน้า …" in their {@code when} — those criteria are about the numbers, and the
     * matrix is what decides who. Recorded as a decision in the build report rather than answered by
     * typing a second role into the route.
     */
    @Test
    void operationsIsRefusedTheQuoteBecauseAclThirteenNamesOnlyTheApplicant() {
        LoanAccount account = disbursedAccount();

        assertThatThrownBy(() -> quotes.quoteFor(account.getId(), OPERATIONS, disbursementDay(account)))
                .isInstanceOf(EarlyClosureQuoteService.ApplicantOnlyException.class);
    }

    /** BR-miniloan-033@v1 — refused at the service, so the URL is no better than the screen. */
    @Test
    void anotherApplicantIsRefusedTheQuote() {
        LoanAccount account = disbursedAccount();
        reassignTo(account.getId(), OPERATIONS);

        assertThatThrownBy(
                        () -> quotes.quoteFor(account.getId(), "ROLE-001-somebody-else", disbursementDay(account)))
                .isInstanceOf(EarlyClosureQuoteService.ApplicantOnlyException.class);
    }

    // ── ACL-014 · AC-miniloan-005 · the payoff that closes the account ───────

    /**
     * AC-miniloan-005 — BR-miniloan-021@v1's second door. The account reads Closed with reason
     * EarlySettlement, and every instalment still Due is retired per BR-miniloan-023@v1 rather than
     * dropped from the table.
     */
    @Test
    void anExactPayoffClosesTheAccountAndRetiresTheRemainingInstalments() {
        LoanAccount account = disbursedAccount();
        LocalDate closingDate = disbursementDay(account);
        Payoff payoff = quotes.payoffOf(account, closingDate);

        var result =
                settlements.settle(
                        account.getId(), OPERATIONS, closingDate, payoff.earlySettlementAmount(), null);

        assertThat(result.account().getStatus()).isEqualTo(LoanAccount.Status.Closed);
        assertThat(result.account().getCloseReason()).isEqualTo(LoanAccount.CloseReason.EarlySettlement);
        assertThat(result.account().getClosedAt()).isNotNull();
        assertThat(Money.exact(result.account().getOutstandingPrincipal())).isEqualTo("0.00");
        assertThat(result.cancelled()).hasSize(12);
        assertThat(currentRows(account.getId()))
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(Installment.Status.Cancelled));
        assertThat(Money.exact(result.payment().getAmount())).isEqualTo("101,000.00");
        assertThat(Money.exact(result.payment().getPrepaymentFee())).isEqualTo("1,000.00");
        assertThat(result.payment().getPaymentType()).isEqualTo(com.miniloan.domain.Payment.PaymentType.EarlySettlement);
    }

    /** Short by one satang is refused whole, and nothing moved. */
    @Test
    void aShortPayoffIsRefusedAndNothingMoves() {
        LoanAccount account = disbursedAccount();
        LocalDate closingDate = disbursementDay(account);
        Payoff payoff = quotes.payoffOf(account, closingDate);

        assertThatThrownBy(
                        () ->
                                settlements.settle(
                                        account.getId(),
                                        OPERATIONS,
                                        closingDate,
                                        payoff.earlySettlementAmount().subtract(new BigDecimal("0.01")),
                                        null))
                .isInstanceOf(EarlyClosureSettlementService.PayoffAmountMismatchException.class);

        assertThat(accounts.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanAccount.Status.Active);
        assertThat(payments.count()).isZero();
        assertThat(currentRows(account.getId()))
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(Installment.Status.Due));
    }

    /** Over by one satang is the same refusal — the system does not take money for nothing. */
    @Test
    void anOverPayoffIsRefusedTheSameWay() {
        LoanAccount account = disbursedAccount();
        LocalDate closingDate = disbursementDay(account);
        Payoff payoff = quotes.payoffOf(account, closingDate);

        assertThatThrownBy(
                        () ->
                                settlements.settle(
                                        account.getId(),
                                        OPERATIONS,
                                        closingDate,
                                        payoff.earlySettlementAmount().add(new BigDecimal("0.01")),
                                        null))
                .isInstanceOf(EarlyClosureSettlementService.PayoffAmountMismatchException.class);

        assertThat(accounts.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanAccount.Status.Active);
        assertThat(payments.count()).isZero();
    }

    /** AC-miniloan-072 — the Applicant may ask for the figure and may not close the account. */
    @Test
    void theApplicantMayNotSettleTheirOwnAccount() {
        LoanAccount account = disbursedAccount();
        LocalDate closingDate = disbursementDay(account);
        Payoff payoff = quotes.payoffOf(account, closingDate);

        assertThatThrownBy(
                        () ->
                                settlements.settle(
                                        account.getId(), APPLICANT, closingDate, payoff.earlySettlementAmount(), null))
                .isInstanceOf(EarlyClosureSettlementService.OperationsOnlyException.class);

        assertThat(accounts.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanAccount.Status.Active);
    }

    /** BR-miniloan-054@v1 — another Operations holding the same role is still refused. */
    @Test
    void anotherOperationsIsRefusedEvenHoldingTheSameRole() {
        LoanAccount account = disbursedAccount();
        LocalDate closingDate = disbursementDay(account);
        Payoff payoff = quotes.payoffOf(account, closingDate);
        reassignTo(account.getId(), ANOTHER_OPERATIONS);

        assertThatThrownBy(
                        () ->
                                settlements.settle(
                                        account.getId(), OPERATIONS, closingDate, payoff.earlySettlementAmount(), null))
                .isInstanceOf(EarlyClosureSettlementService.NotAssignedOperationsException.class);
    }

    /**
     * BR-miniloan-043@v1 asks for a dedup key on every write and one is claimed, but the guard that
     * actually answers a repeated payoff is the STATE: the first settlement closed the account, so
     * the second is refused before the key is ever reached. The key stays because the rule asks for
     * it and because the two guards fail differently under concurrency — the state check reads a row
     * this transaction has not locked, and the unique constraint is what makes the write itself
     * idempotent. Recorded in the build report: {@code DuplicateCommandException} has no single-
     * threaded path to it here, unlike API-014's.
     */
    @Test
    void theSameSettlementCommandTwiceIsRefusedByTheClosedState() {
        LoanAccount account = disbursedAccount();
        LocalDate closingDate = disbursementDay(account);
        Payoff payoff = quotes.payoffOf(account, closingDate);

        settlements.settle(
                account.getId(), OPERATIONS, closingDate, payoff.earlySettlementAmount(), "op-payoff-0001");

        assertThatThrownBy(
                        () ->
                                settlements.settle(
                                        account.getId(),
                                        OPERATIONS,
                                        closingDate,
                                        payoff.earlySettlementAmount(),
                                        "op-payoff-0001"))
                .isInstanceOf(EarlyClosureSettlementService.AccountClosedException.class);
    }

    /**
     * BR-miniloan-045@v1 — a closed account takes no further settlement, and the refusal comes from
     * the state rather than from the amount.
     */
    @Test
    void aClosedAccountRefusesASecondPayoff() {
        LoanAccount account = disbursedAccount();
        LocalDate closingDate = disbursementDay(account);
        Payoff payoff = quotes.payoffOf(account, closingDate);
        settlements.settle(
                account.getId(), OPERATIONS, closingDate, payoff.earlySettlementAmount(), null);

        assertThatThrownBy(
                        () ->
                                settlements.settle(
                                        account.getId(), OPERATIONS, closingDate, payoff.earlySettlementAmount(), "again"))
                .isInstanceOf(EarlyClosureSettlementService.AccountClosedException.class);
    }

    /**
     * CALC-miniloan-004@v1's input list: the principal is the schedule's balance column, so once an
     * instalment is settled the payoff is measured from that row and from its due date — not from the
     * disbursement date and not from a figure recomputed by another formula.
     */
    @Test
    void afterOneInstalmentThePayoffIsMeasuredFromThatRow() {
        LoanAccount account = disbursedAccount();
        reassignTo(account.getId(), OPERATIONS);
        Installment first = currentRows(account.getId()).get(0);
        paymentService.record(account.getId(), OPERATIONS, 1, first.getEmiAmount(), null);

        Installment settled = currentRows(account.getId()).get(0);
        Payoff payoff = quotes.payoffOf(accounts.findById(account.getId()).orElseThrow(), settled.getDueDate());

        assertThat(payoff.daysElapsed()).isZero();
        assertThat(Money.exact(payoff.remainingPrincipal()))
                .isEqualTo(Money.exact(settled.getRemainingBalance()));
        assertThat(Money.exact(payoff.accruedInterest())).isEqualTo("0.00");
    }
}

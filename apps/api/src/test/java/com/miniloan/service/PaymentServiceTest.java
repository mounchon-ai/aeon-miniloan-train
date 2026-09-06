package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.Payment;
import com.miniloan.domain.RepaymentSchedule;
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
import com.miniloan.service.PaymentService.AccountClosedException;
import com.miniloan.service.PaymentService.AlreadyClosedException;
import com.miniloan.service.PaymentService.DirectCloseRefusedException;
import com.miniloan.service.PaymentService.DuplicateCommandException;
import com.miniloan.service.PaymentService.NotAssignedOperationsException;
import com.miniloan.service.PaymentService.OperationsOnlyException;
import com.miniloan.service.PaymentService.PartialPaymentException;
import com.miniloan.service.PaymentService.PaymentResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
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
 * บันทึกการชำระ · ปิดบัญชีอัตโนมัติ · โปะเงินต้น (UC-miniloan-012 · 013 · 014 · 024) —
 * AC-miniloan-004 · 006 · 007 · 012 · 013 · 014 · 069 · 070 · 086 · 087 · 088 · 135 · 136 · 137.
 *
 * <p><b>The instalment figures are not invented here.</b> Every account in this file is
 * AC-miniloan-004's and AC-miniloan-086's own — 100,000 บาท · 12 งวด · 25% ต่อปี — which is
 * GD-miniloan-002's first row, and the table it is paid against was written by
 * CALC-miniloan-001@v2 at disbursement (FE-miniloan-010's proof). What is measured here is what a
 * PAYMENT does to that signed table, so the expected amounts are read back off the row rather than
 * recomputed: "the instalment's own emiAmount" is the only figure a payment is compared against, and
 * writing a literal for it would be this file grading its own homework.
 *
 * <p><b>AC-miniloan-005 is not in this file, and that is deliberate.</b> The early-settlement
 * payment of UC-miniloan-016 has to be checked against the payoff amount CALC-miniloan-004@v1
 * defines and GD-miniloan-005 signs — and that calculation belongs to FE-miniloan-014, which this
 * unit does not depend on. See {@link PaymentService}'s note and the build report.
 *
 * <p><b>Two things are arranged rather than driven</b>, the way {@code AmortizationScheduleReissueTest}
 * arranges a closed account: an account assigned to a DIFFERENT Operations person, because
 * {@code DisbursementService} hard-codes the mock scheme's single Operations identity, and there is
 * no reassignment command anywhere in this build plan (BR-miniloan-054@v1 leaves it undecided). The
 * guard it exercises is real; only the setup is written straight onto the row.
 */
@SpringBootTest
class PaymentServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ADMIN = "ROLE-005";

    /** AC-miniloan-136's "Operations ข." — a different person holding the very same role. */
    private static final String ANOTHER_OPERATIONS = "ROLE-004-another-person";

    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    @Autowired private PaymentService paymentService;
    @Autowired private LoanAccountScopeService scopeService;
    @Autowired private AmortizationScheduleService amortization;
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

    // ── setup ────────────────────────────────────────────────────────────────

    /** AC-miniloan-004's and AC-miniloan-086's account: 100,000 บาท · 12 งวด · 25% ต่อปี. */
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

    /** BR-miniloan-054@v1's other side — see the class note on why this is arranged, not driven. */
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
        RepaymentSchedule current =
                schedules.findByLoanAccountIdAndCurrentIsTrue(accountId).orElseThrow();
        return installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId());
    }

    private Installment row(UUID accountId, int number) {
        return currentRows(accountId).get(number - 1);
    }

    /** Settle instalments 1..n exactly, each through the service — no state is written by hand. */
    private void payExactly(UUID accountId, int upToInstallment) {
        for (int number = 1; number <= upToInstallment; number++) {
            paymentService.record(accountId, OPERATIONS, number, row(accountId, number).getEmiAmount(), null);
        }
    }

    private LoanAccount reload(UUID accountId) {
        return accounts.findById(accountId).orElseThrow();
    }

    // ── AC-miniloan-013 · ชำระตรงยอดงวด ───────────────────────────────────────

    /**
     * "ค่าที่ตรงเส้นพอดี ซึ่งเป็นฝั่งที่ผ่าน" — instalment 3 becomes Paid and the balance falls by
     * that row's PRINCIPAL, not by the amount handed over (BR-miniloan-020@v1).
     */
    @Test
    void payingAnInstallmentExactlySettlesItAndReducesTheBalanceByItsPrincipal() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 2);
        BigDecimal before = reload(account.getId()).getOutstandingPrincipal();
        Installment third = row(account.getId(), 3);

        PaymentResult result =
                paymentService.record(account.getId(), OPERATIONS, 3, third.getEmiAmount(), null);

        assertThat(result.installment().getStatus()).isEqualTo(Installment.Status.Paid);
        assertThat(result.installment().getPaidAt()).isNotNull();
        assertThat(result.payment().getPaymentType()).isEqualTo(Payment.PaymentType.InstallmentExact);
        assertThat(result.payment().getOverpaymentAmount()).isNull();
        assertThat(result.payment().getPrepaymentFee()).isNull();
        assertThat(result.closed()).isFalse();
        assertThat(reload(account.getId()).getOutstandingPrincipal())
                .isEqualByComparingTo(before.subtract(third.getPrincipalPortion()));
        assertThat(row(account.getId(), 3).getStatus()).isEqualTo(Installment.Status.Paid);
    }

    // ── AC-miniloan-012 · AC-miniloan-014 · ไม่รับชำระบางส่วน ──────────────────

    /** "ขาดแม้สตางค์เดียวก็ไม่ผ่าน ไม่มีค่าผ่อนผัน" — short by 0.01 and nothing moves. */
    @Test
    void payingOneSatangShortIsRefusedAndNothingMoves() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 2);
        BigDecimal before = reload(account.getId()).getOutstandingPrincipal();
        Installment third = row(account.getId(), 3);
        BigDecimal short01 = third.getEmiAmount().subtract(new BigDecimal("0.01"));

        assertThatThrownBy(() -> paymentService.record(account.getId(), OPERATIONS, 3, short01, null))
                .isInstanceOf(PartialPaymentException.class)
                .hasMessage(
                        "ยอดชำระไม่ครบงวด — งวดที่ 3 ต้องชำระเต็มจำนวน "
                                + com.miniloan.domain.Money.exact(third.getEmiAmount())
                                + " บาท ระบบไม่รับชำระบางส่วน");

        assertThat(row(account.getId(), 3).getStatus()).isEqualTo(Installment.Status.Due);
        assertThat(reload(account.getId()).getOutstandingPrincipal()).isEqualByComparingTo(before);
    }

    /**
     * AC-miniloan-014: two halves that add up to the instalment are refused SEPARATELY, nothing
     * accumulates, and there is no partially-paid state to land in — measured as no Payment row for
     * instalment 3 at all after both attempts.
     */
    @Test
    void twoHalfPaymentsAreEachRefusedAndNothingAccumulates() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 2);
        BigDecimal before = reload(account.getId()).getOutstandingPrincipal();
        Installment third = row(account.getId(), 3);
        BigDecimal half = third.getEmiAmount().divide(new BigDecimal("2"), 2, java.math.RoundingMode.DOWN);

        for (int attempt = 1; attempt <= 2; attempt++) {
            assertThatThrownBy(() -> paymentService.record(account.getId(), OPERATIONS, 3, half, null))
                    .isInstanceOf(PartialPaymentException.class);
        }

        assertThat(row(account.getId(), 3).getStatus()).isEqualTo(Installment.Status.Due);
        assertThat(reload(account.getId()).getOutstandingPrincipal()).isEqualByComparingTo(before);
        assertThat(payments.findByLoanAccountIdOrderByRecordedAtAsc(account.getId()))
                .extracting(Payment::getInstallmentId)
                .doesNotContain(third.getId());
        assertThat(payments.findByLoanAccountIdOrderByRecordedAtAsc(account.getId())).hasSize(2);
    }

    // ── AC-miniloan-004 · AC-miniloan-069 · ปิดบัญชีเมื่อชำระครบทุกงวด ─────────

    /**
     * AC-miniloan-004 · AC-miniloan-069: eleven paid, the twelfth settles it, and the account closes
     * itself. Every instalment reads Paid, the reason is FullyPaid, and closedAt is the moment the
     * last payment was recorded — nobody pressed a close button.
     */
    @Test
    void payingTheLastInstallmentClosesTheAccountByItself() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 11);
        assertThat(reload(account.getId()).getStatus()).isEqualTo(LoanAccount.Status.Active);

        PaymentResult result =
                paymentService.record(
                        account.getId(), OPERATIONS, 12, row(account.getId(), 12).getEmiAmount(), null);

        assertThat(result.closed()).isTrue();
        LoanAccount closed = reload(account.getId());
        assertThat(closed.getStatus()).isEqualTo(LoanAccount.Status.Closed);
        assertThat(closed.getCloseReason()).isEqualTo(LoanAccount.CloseReason.FullyPaid);
        // Both sides read back off the row: the column keeps microseconds and the in-memory Instant
        // carries nanoseconds, so comparing one against the other measures the column's precision
        // rather than the rule. Same remedy FE-miniloan-010 recorded for the disbursement time.
        assertThat(closed.getClosedAt())
                .isEqualTo(payments.findById(result.payment().getId()).orElseThrow().getRecordedAt());
        assertThat(closed.getOutstandingPrincipal()).isEqualByComparingTo("0.00");
        assertThat(currentRows(account.getId()))
                .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(Installment.Status.Paid));
    }

    // ── AC-miniloan-086 · AC-miniloan-087 · โปะเงินต้น ────────────────────────

    @Nested
    @DisplayName("BR-miniloan-046@v2 — เกินยอดงวดแม้สตางค์เดียวก็เข้าเส้นโปะ")
    class Overpayment {

        /**
         * AC-miniloan-086, with its own arithmetic: an excess of 20,000.00 carries a fee of 200.00
         * and cuts the principal by 19,800.00 — "ไม่ใช่ 20,000.00 บาท", which is the whole point of
         * the criterion and the reason the fee is asserted separately from the cut.
         */
        @Test
        void anExcessOfTwentyThousandCostsTwoHundredAndCutsNineteenEightHundred() {
            LoanAccount account = disbursedAccount();
            payExactly(account.getId(), 5);
            BigDecimal before = reload(account.getId()).getOutstandingPrincipal();
            Installment sixth = row(account.getId(), 6);
            BigDecimal excess = new BigDecimal("20000.00");

            PaymentResult result =
                    paymentService.record(
                            account.getId(), OPERATIONS, 6, sixth.getEmiAmount().add(excess), null);

            assertThat(result.payment().getPaymentType())
                    .isEqualTo(Payment.PaymentType.InstallmentOverpayment);
            assertThat(result.payment().getOverpaymentAmount()).isEqualByComparingTo("20000.00");
            assertThat(result.payment().getPrepaymentFee()).isEqualByComparingTo("200.00");
            assertThat(result.principalReduction())
                    .isEqualByComparingTo(sixth.getPrincipalPortion().add(new BigDecimal("19800.00")));
            assertThat(reload(account.getId()).getOutstandingPrincipal())
                    .isEqualByComparingTo(
                            before.subtract(sixth.getPrincipalPortion()).subtract(new BigDecimal("19800.00")));
            assertThat(result.installment().getStatus()).isEqualTo(Installment.Status.Paid);
            assertThat(result.closed()).isFalse();
        }

        /**
         * AC-miniloan-086's second half: the table is replaced on the spot (BR-miniloan-044@v1), the
         * instalment stays exactly what the borrower was already paying, and the table ends sooner
         * than the six instalments that were left.
         */
        @Test
        void theTableIsReissuedAtTheSameInstalmentAndEndsSooner() {
            LoanAccount account = disbursedAccount();
            payExactly(account.getId(), 5);
            Installment sixth = row(account.getId(), 6);
            BigDecimal instalment = sixth.getEmiAmount();

            PaymentResult result =
                    paymentService.record(
                            account.getId(),
                            OPERATIONS,
                            6,
                            instalment.add(new BigDecimal("20000.00")),
                            null);

            assertThat(result.reissued()).isPresent();
            assertThat(result.reissued().orElseThrow().getRevisionNumber()).isEqualTo(2);

            List<Installment> reissuedRows = currentRows(account.getId());
            assertThat(reissuedRows).hasSizeLessThan(6).isNotEmpty();
            // "ค่างวดยังเท่าเดิมทุกงวด" — every row but the last, which absorbs the residual exactly
            // as CALC-miniloan-001@v2's residual_policy already allows.
            assertThat(reissuedRows.subList(0, reissuedRows.size() - 1))
                    .allSatisfy(r -> assertThat(r.getEmiAmount()).isEqualByComparingTo(instalment));
            assertThat(reissuedRows.get(reissuedRows.size() - 1).getRemainingBalance())
                    .isEqualByComparingTo("0.00");
            assertThat(
                            reissuedRows.stream()
                                    .map(Installment::getPrincipalPortion)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isEqualByComparingTo(reload(account.getId()).getOutstandingPrincipal());

            // BR-miniloan-044@v1: revision 1 is kept, not erased.
            assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId())).hasSize(2);
        }

        /**
         * AC-miniloan-087: over by 0.01 — the fee is 1% of 0.01 = 0.0001, which HALF_UP at two places
         * is 0.00, so the whole satang reaches the principal. "เกินแม้สตางค์เดียวก็เข้าเส้นโปะ ไม่ใช่
         * เศษที่ปัดทิ้ง" — the reissue happens too. The number of decimal places is
         * DQ-miniloan-001's to settle; two is what BR-miniloan-035@v1 gives today.
         */
        @Test
        void overByOneSatangIsAnOverpaymentWithAZeroFee() {
            LoanAccount account = disbursedAccount();
            payExactly(account.getId(), 5);
            BigDecimal before = reload(account.getId()).getOutstandingPrincipal();
            Installment sixth = row(account.getId(), 6);

            PaymentResult result =
                    paymentService.record(
                            account.getId(),
                            OPERATIONS,
                            6,
                            sixth.getEmiAmount().add(new BigDecimal("0.01")),
                            null);

            assertThat(result.payment().getPaymentType())
                    .isEqualTo(Payment.PaymentType.InstallmentOverpayment);
            assertThat(result.payment().getOverpaymentAmount()).isEqualByComparingTo("0.01");
            assertThat(result.payment().getPrepaymentFee()).isEqualByComparingTo("0.00");
            assertThat(reload(account.getId()).getOutstandingPrincipal())
                    .isEqualByComparingTo(
                            before.subtract(sixth.getPrincipalPortion()).subtract(new BigDecimal("0.01")));
            assertThat(result.reissued()).isPresent();
        }

        /**
         * The branch no acceptance criterion covers: an overpayment large enough to clear the
         * principal exactly. It must NOT reissue — a table over a principal of nothing is refused by
         * the calculator — so the account closes instead, its remaining rows are retired the way
         * BR-miniloan-023@v1 retires them, and the reason recorded is FullyPaid. See
         * {@link PaymentService}'s note: this is decided here and raised for design, not inferred.
         */
        @Test
        void anOverpaymentThatClearsThePrincipalClosesTheAccountWithoutANewTable() {
            LoanAccount account = disbursedAccount();
            payExactly(account.getId(), 5);
            Installment sixth = row(account.getId(), 6);
            BigDecimal owedPrincipalAfterThisRow =
                    reload(account.getId()).getOutstandingPrincipal().subtract(sixth.getPrincipalPortion());
            // The excess has to survive the 1% fee and land exactly on the remaining principal:
            // cut = excess − round(excess × 1%), so excess = remaining / 0.99 solved at two places.
            BigDecimal excess =
                    owedPrincipalAfterThisRow.divide(new BigDecimal("0.99"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal cut = excess.subtract(excess.multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP));
            // Nudge to land on zero exactly whichever way the two roundings fall.
            excess = excess.add(owedPrincipalAfterThisRow.subtract(cut));

            PaymentResult result =
                    paymentService.record(
                            account.getId(), OPERATIONS, 6, sixth.getEmiAmount().add(excess), null);

            LoanAccount closed = reload(account.getId());
            assertThat(closed.getOutstandingPrincipal()).isEqualByComparingTo("0.00");
            assertThat(result.closed()).isTrue();
            assertThat(result.reissued()).isEmpty();
            assertThat(closed.getStatus()).isEqualTo(LoanAccount.Status.Closed);
            assertThat(closed.getCloseReason()).isEqualTo(LoanAccount.CloseReason.FullyPaid);
            assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId())).hasSize(1);
            assertThat(currentRows(account.getId()))
                    .filteredOn(r -> r.getInstallmentNumber() > 6)
                    .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(Installment.Status.Cancelled));
        }

        /** AC-miniloan-088: the account is closed, so there is nothing left to overpay into. */
        @Test
        void anOverpaymentIntoAClosedAccountIsRefusedAndIssuesNoNewTable() {
            LoanAccount account = disbursedAccount();
            payExactly(account.getId(), 12);
            assertThat(reload(account.getId()).getStatus()).isEqualTo(LoanAccount.Status.Closed);
            int revisionsBefore =
                    schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId()).size();

            assertThatThrownBy(
                            () ->
                                    paymentService.record(
                                            account.getId(), OPERATIONS, 12, new BigDecimal("5000.00"), null))
                    .isInstanceOf(AccountClosedException.class)
                    .hasMessage("บัญชีนี้ปิดแล้ว — บันทึกการชำระเพิ่มไม่ได้");

            assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId()))
                    .hasSize(revisionsBefore);
        }
    }

    // ── AC-miniloan-070 · BR-miniloan-034@v1 · ใครบันทึกได้ ───────────────────

    /**
     * AC-miniloan-070: the Applicant OWNS this account and still cannot record a payment on it —
     * ownership is not the question BR-miniloan-034@v1 asks. Every other non-Operations role is
     * refused the same way.
     */
    @Test
    void nobodyButOperationsMayRecordAPayment() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 2);
        BigDecimal before = reload(account.getId()).getOutstandingPrincipal();
        BigDecimal owed = row(account.getId(), 3).getEmiAmount();

        for (String role : List.of(APPLICANT, OFFICER, SUPERVISOR, ADMIN)) {
            assertThatThrownBy(() -> paymentService.record(account.getId(), role, 3, owed, null))
                    .isInstanceOf(OperationsOnlyException.class)
                    .hasMessage("ไม่มีสิทธิ์บันทึกการชำระ — การบันทึกการชำระทำได้เฉพาะเจ้าหน้าที่ Operations");
        }

        assertThat(row(account.getId(), 3).getStatus()).isEqualTo(Installment.Status.Due);
        assertThat(reload(account.getId()).getOutstandingPrincipal()).isEqualByComparingTo(before);
    }

    // ── AC-miniloan-135 · AC-miniloan-136 · BR-miniloan-054@v1 · บัญชีของใคร ──

    /** AC-miniloan-135: the Operations person the account IS assigned to is served normally. */
    @Test
    void theAssignedOperationsPersonMayRecordAPayment() {
        LoanAccount account = disbursedAccount();

        PaymentResult result =
                paymentService.record(
                        account.getId(), OPERATIONS, 1, row(account.getId(), 1).getEmiAmount(), null);

        assertThat(result.installment().getStatus()).isEqualTo(Installment.Status.Paid);
        assertThat(result.payment().getRecordedBy()).isEqualTo(OPERATIONS);
    }

    /**
     * AC-miniloan-136: the same role, a different person, an account they were not assigned — both
     * the payment and the close command are refused with the criterion's sentence, and account Y is
     * unchanged in balance and in status.
     */
    @Test
    void anUnassignedOperationsIsRefusedBothCommandsAndTheAccountIsUntouched() {
        LoanAccount other = disbursedAccount();
        reassignTo(other.getId(), ANOTHER_OPERATIONS);
        BigDecimal before = reload(other.getId()).getOutstandingPrincipal();
        BigDecimal owed = row(other.getId(), 1).getEmiAmount();

        assertThatThrownBy(() -> paymentService.record(other.getId(), OPERATIONS, 1, owed, null))
                .isInstanceOf(NotAssignedOperationsException.class)
                .hasMessage("ไม่มีสิทธิ์ดำเนินการกับบัญชีสินเชื่อนี้ — ไม่ได้ถูก assign ดูแลบัญชีนี้");

        assertThatThrownBy(() -> paymentService.refuseDirectClose(other.getId(), OPERATIONS))
                .isInstanceOf(NotAssignedOperationsException.class)
                .hasMessage("ไม่มีสิทธิ์ดำเนินการกับบัญชีสินเชื่อนี้ — ไม่ได้ถูก assign ดูแลบัญชีนี้");

        LoanAccount untouched = reload(other.getId());
        assertThat(untouched.getOutstandingPrincipal()).isEqualByComparingTo(before);
        assertThat(untouched.getStatus()).isEqualTo(LoanAccount.Status.Active);
        assertThat(row(other.getId(), 1).getStatus()).isEqualTo(Installment.Status.Due);
        assertThat(payments.findByLoanAccountIdOrderByRecordedAtAsc(other.getId())).isEmpty();
    }

    /**
     * AC-miniloan-137, the list. The criterion is explicit that the hole lives here rather than in
     * the single read, so the COUNT is asserted as well as the contents — a list that merely
     * contains the caller's own account also passes when it contains everybody else's.
     */
    @Test
    void theAccountListShowsOnlyTheAccountsThisOperationsPersonWasAssigned() {
        LoanAccount mine = disbursedAccount();
        LoanAccount theirs = disbursedAccount();
        LoanAccount alsoTheirs = disbursedAccount();
        reassignTo(theirs.getId(), ANOTHER_OPERATIONS);
        reassignTo(alsoTheirs.getId(), "ROLE-004-a-third-person");

        List<LoanAccount> visible = scopeService.visibleTo(OPERATIONS);

        assertThat(visible).hasSize(1);
        assertThat(visible).extracting(LoanAccount::getId).containsExactly(mine.getId());
        assertThat(accounts.count()).isEqualTo(3);
    }

    /** The single read answers the same question the list does, so the two cannot disagree. */
    @Test
    void readingOneUnassignedAccountIsRefusedToo() {
        LoanAccount theirs = disbursedAccount();
        reassignTo(theirs.getId(), ANOTHER_OPERATIONS);

        assertThatThrownBy(() -> scopeService.visibleTo(OPERATIONS, theirs.getId()))
                .isInstanceOf(LoanAccountScopeService.NotVisibleException.class);
        assertThat(scopeService.visibleTo(APPLICANT)).extracting(LoanAccount::getId)
                .containsExactly(theirs.getId());
    }

    // ── AC-miniloan-006 · AC-miniloan-007 · ปิดบัญชีไม่ใช่คำสั่ง ──────────────

    /** AC-miniloan-006, word for word, with the seven instalments still owing counted. */
    @Test
    void closingAnAccountThatStillOwesIsRefusedAndTheAccountStaysActive() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 5);

        assertThatThrownBy(() -> paymentService.refuseDirectClose(account.getId(), OPERATIONS))
                .isInstanceOf(DirectCloseRefusedException.class)
                .hasMessage(
                        "ปิดบัญชีไม่ได้ — บัญชีนี้ยังมีงวดค้าง 7 งวด · ปิดบัญชีได้เมื่อชำระครบทุกงวด"
                                + " หรือชำระยอดปิดบัญชีก่อนกำหนดครบเท่านั้น");

        assertThat(reload(account.getId()).getStatus()).isEqualTo(LoanAccount.Status.Active);
    }

    /**
     * BR-miniloan-034@v1's second half — "บันทึก Payment <b>และปิดบัญชี</b>สินเชื่อทำได้เฉพาะ
     * Operations". No criterion quotes a sentence for it, so the refusal has one of its own rather
     * than borrowing the payment sentence: a caller asking to CLOSE should not be told what they may
     * not RECORD.
     */
    @Test
    void nobodyButOperationsMayEvenAskToCloseAnAccount() {
        LoanAccount account = disbursedAccount();

        for (String role : List.of(APPLICANT, OFFICER, SUPERVISOR, ADMIN)) {
            assertThatThrownBy(() -> paymentService.refuseDirectClose(account.getId(), role))
                    .isInstanceOf(PaymentService.CloseOperationsOnlyException.class)
                    .hasMessage("ไม่มีสิทธิ์ปิดบัญชีสินเชื่อ — การปิดบัญชีทำได้เฉพาะเจ้าหน้าที่ Operations");
        }

        assertThat(reload(account.getId()).getStatus()).isEqualTo(LoanAccount.Status.Active);
    }

    /** AC-miniloan-007: Closed is terminal, so closing it again is refused and nothing moves. */
    @Test
    void closingAnAlreadyClosedAccountIsRefusedAndTheCloseTimeIsNotMoved() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 12);
        var closedAt = reload(account.getId()).getClosedAt();

        assertThatThrownBy(() -> paymentService.refuseDirectClose(account.getId(), OPERATIONS))
                .isInstanceOf(AlreadyClosedException.class)
                .hasMessage("บัญชีนี้ปิดแล้ว — ปิดซ้ำไม่ได้");

        LoanAccount still = reload(account.getId());
        assertThat(still.getStatus()).isEqualTo(LoanAccount.Status.Closed);
        assertThat(still.getClosedAt()).isEqualTo(closedAt);
    }

    /** BR-miniloan-021@v1 at the layer that owns it: the entity itself refuses a second close. */
    @Test
    void theEntityRefusesToBeClosedTwice() {
        LoanAccount account = disbursedAccount();
        payExactly(account.getId(), 12);
        LoanAccount closed = reload(account.getId());

        assertThatThrownBy(
                        () -> closed.close(LoanAccount.CloseReason.EarlySettlement, java.time.Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── BR-miniloan-043@v1 · การยิงซ้ำ ───────────────────────────────────────

    /**
     * The same command twice is refused by the database, not by a disabled button — and the refusal
     * is a named error the caller sees, never the first result handed back silently.
     */
    @Test
    void repeatingTheSamePaymentCommandIsRefusedByTheKey() {
        LoanAccount account = disbursedAccount();
        BigDecimal owed = row(account.getId(), 1).getEmiAmount();
        paymentService.record(account.getId(), OPERATIONS, 1, owed, "op-2026-09-06-0001");

        assertThatThrownBy(
                        () -> paymentService.record(account.getId(), OPERATIONS, 2, owed, "op-2026-09-06-0001"))
                .isInstanceOf(DuplicateCommandException.class);

        assertThat(row(account.getId(), 2).getStatus()).isEqualTo(Installment.Status.Due);
    }

    /** Without a client key the instalment itself is the key, and one instalment settles once. */
    @Test
    void payingTheSameInstallmentTwiceWithoutAKeyIsRefused() {
        LoanAccount account = disbursedAccount();
        BigDecimal owed = row(account.getId(), 1).getEmiAmount();
        paymentService.record(account.getId(), OPERATIONS, 1, owed, null);

        assertThatThrownBy(() -> paymentService.record(account.getId(), OPERATIONS, 1, owed, null))
                .isInstanceOf(PaymentService.InstallmentNotDueException.class);

        assertThat(payments.findByLoanAccountIdOrderByRecordedAtAsc(account.getId())).hasSize(1);
    }

    // ── the fixed-instalment table itself ────────────────────────────────────

    @Nested
    @DisplayName("BR-miniloan-046@v2 — ตารางที่ค่างวดคงเดิมและงวดลดลง")
    class FixedInstalmentTable {

        /**
         * No golden dataset signs a fixed-instalment table, because CALC-miniloan-001@v2 answers the
         * other question. What is asserted is therefore that this table obeys the contract's OWN
         * invariants — the sum of principal is the principal (BR-miniloan-017@v1), the balance closes
         * at exactly 0.00, and every row's interest is the balance before it times the monthly rate,
         * rounded once.
         */
        @Test
        void everyRowFollowsTheContractsRecurrenceAndTheTableClosesAtZero() {
            BigDecimal principal = new BigDecimal("80200.00");
            BigDecimal instalment = new BigDecimal("9483.29");

            var table =
                    amortization.buildAtFixedInstalment(principal, new BigDecimal("0.25"), instalment);

            assertThat(table.totalPrincipal()).isEqualByComparingTo(principal);
            assertThat(table.rows().get(table.rows().size() - 1).remainingBalance())
                    .isEqualByComparingTo("0.00");

            BigDecimal monthlyRate =
                    new BigDecimal("0.25").divide(new BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP);
            BigDecimal balance = principal;
            for (var r : table.rows()) {
                assertThat(r.interest())
                        .isEqualByComparingTo(
                                balance.multiply(monthlyRate).setScale(2, java.math.RoundingMode.HALF_UP));
                assertThat(r.instalment()).isEqualByComparingTo(r.interest().add(r.principal()));
                balance = balance.subtract(r.principal());
            }
            assertThat(balance).isEqualByComparingTo("0.00");
        }

        /** Same principal, same rate: a bigger instalment ends the table sooner, never later. */
        @Test
        void aLargerInstalmentShortensTheTable() {
            BigDecimal principal = new BigDecimal("80200.00");
            BigDecimal rate = new BigDecimal("0.25");

            int shorter = amortization.buildAtFixedInstalment(principal, rate, new BigDecimal("15000.00")).rows().size();
            int longer = amortization.buildAtFixedInstalment(principal, rate, new BigDecimal("9483.29")).rows().size();

            assertThat(shorter).isLessThan(longer);
        }

        /** An instalment that cannot cover the first month's interest is refused, never looped. */
        @Test
        void anInstalmentThatCannotCoverTheInterestIsRefused() {
            assertThatThrownBy(
                            () ->
                                    amortization.buildAtFixedInstalment(
                                            new BigDecimal("100000.00"), new BigDecimal("0.25"), new BigDecimal("100.00")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

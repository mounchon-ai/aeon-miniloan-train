package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.Money;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.RepaymentScheduleReissueService.AccountClosedException;
import com.miniloan.service.RepaymentScheduleReissueService.NotAssignedOperationsException;
import com.miniloan.service.RepaymentScheduleReissueService.Reissue;
import com.miniloan.service.RepaymentScheduleReissueService.Revision;
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
 * ออกตารางผ่อนฉบับใหม่ทับ (UC-miniloan-010 · BR-miniloan-044@v1 · BR-miniloan-045@v1) —
 * AC-miniloan-008 · 009 · 010 · 011 · 107 · 108 · 109.
 *
 * <p>The arithmetic is CALC-miniloan-001@v2's and was signed as GD-miniloan-002; what this file adds
 * is that the table a REISSUE persists is that same signed table, read back out of the database
 * rather than off the calculator's return value. AC-miniloan-008's account — 100,000 บาท · 12 งวด ·
 * 25% ต่อปี — is GD-miniloan-002's first row exactly, so the two line up with no value invented
 * here.
 *
 * <p><b>Closing an account is arranged, not driven.</b> {@code LoanAccount} exposes no mutator for
 * its status and nothing in this codebase can close one — FE-miniloan-013 is the unit that records
 * the final payment and closes the account. AC-miniloan-107/108/109 are about what the reissue guard
 * does once an account IS closed, so the Closed state is written straight onto the row through JPQL
 * and the guard is measured against it. Building a {@code close()} to make the test convenient would
 * be building part of FE-miniloan-013 inside this unit.
 */
@SpringBootTest
class AmortizationScheduleReissueTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ANOTHER_OPERATIONS = "STAFF-ค";

    /** The rate GD-miniloan-002 computed every row at, as ENT-005 stores it (per cent per year). */
    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    @Autowired private RepaymentScheduleReissueService reissueService;
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

    private InterestRateVersion publish(BigDecimal annualRatePercent, LocalDate effectiveFrom) {
        return rateVersions.save(new InterestRateVersion(annualRatePercent, effectiveFrom, SUPERVISOR));
    }

    /** AC-miniloan-008's account: 100,000 บาท over 12 instalments at 25% — GD-miniloan-002 row 1. */
    private LoanAccount disbursedAccount() {
        publish(RATE_25_PERCENT, LocalDate.now().minusYears(1));
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

    /**
     * The one thing FE-miniloan-013 will do that this unit cannot — see the class note. Written
     * through JPQL so no production class gains a mutator it has no requirement for yet.
     */
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

    private List<Installment> rowsOf(RepaymentSchedule schedule) {
        return installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(schedule.getId());
    }

    private RepaymentSchedule revision(UUID accountId, int revisionNumber) {
        return schedules.findByLoanAccountIdOrderByRevisionNumberAsc(accountId).stream()
                .filter(s -> s.getRevisionNumber() == revisionNumber)
                .findFirst()
                .orElseThrow();
    }

    // ── AC-miniloan-008 · the ordinary case ────────────────────────────────────

    /**
     * AC-miniloan-008: revision 2 becomes the table in use, revision 1 stays readable and is marked
     * as replaced at the moment revision 2 was issued.
     */
    @Test
    void reissuingMakesTheNextRevisionCurrentAndDatesTheOneItReplaced() {
        LoanAccount account = disbursedAccount();

        Reissue reissued = reissueService.reissue(account.getId(), OPERATIONS);

        assertThat(reissued.schedule().getRevisionNumber()).isEqualTo(2);
        assertThat(reissued.schedule().isCurrent()).isTrue();
        assertThat(revision(account.getId(), 1).isCurrent()).isFalse();

        assertThat(reissued.revisions()).hasSize(2);
        Revision first = reissued.revisions().get(0);
        assertThat(first.schedule().getRevisionNumber()).isEqualTo(1);
        assertThat(first.supersededAt()).isEqualTo(reissued.schedule().getIssuedAt());
        assertThat(reissued.revisions().get(1).supersededAt()).isNull();
    }

    /**
     * AC-miniloan-008 · AC-miniloan-010: "เปิดฉบับที่ 1 ขึ้นมาดูได้ครบทุกแถว" — the replaced revision
     * keeps every row with every figure it was issued with. Snapshotted before the reissue and
     * compared after, so a silent rewrite would show up as a difference rather than as a row count.
     */
    @Test
    void theReplacedRevisionKeepsEveryRowUntouched() {
        LoanAccount account = disbursedAccount();
        RepaymentSchedule original = revision(account.getId(), 1);
        List<String> before =
                rowsOf(original).stream().map(AmortizationScheduleReissueTest::fingerprint).toList();

        reissueService.reissue(account.getId(), OPERATIONS);

        List<String> after =
                rowsOf(revision(account.getId(), 1)).stream()
                        .map(AmortizationScheduleReissueTest::fingerprint)
                        .toList();
        assertThat(after).hasSize(12).isEqualTo(before);
        assertThat(revision(account.getId(), 1).getTotalPrincipal())
                .isEqualByComparingTo(original.getTotalPrincipal());
    }

    private static String fingerprint(Installment row) {
        return row.getInstallmentNumber()
                + "|"
                + row.getDueDate()
                + "|"
                + Money.exact(row.getEmiAmount())
                + "|"
                + Money.exact(row.getInterestPortion())
                + "|"
                + Money.exact(row.getPrincipalPortion())
                + "|"
                + Money.exact(row.getRemainingBalance())
                + "|"
                + row.getStatus();
    }

    // ── AC-miniloan-011 · the boundary is REPEATED reissue ─────────────────────

    /**
     * AC-miniloan-011: a third revision over the second — no revision disappears, and exactly one is
     * ever marked as in use.
     */
    @Test
    void reissuingAgainGivesRevisionThreeAndOnlyOneRevisionIsEverCurrent() {
        LoanAccount account = disbursedAccount();

        reissueService.reissue(account.getId(), OPERATIONS);
        Reissue third = reissueService.reissue(account.getId(), OPERATIONS);

        assertThat(third.schedule().getRevisionNumber()).isEqualTo(3);
        List<RepaymentSchedule> all = schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId());
        assertThat(all).extracting(RepaymentSchedule::getRevisionNumber).containsExactly(1, 2, 3);
        assertThat(all).filteredOn(RepaymentSchedule::isCurrent).hasSize(1);
        assertThat(schedules.findByLoanAccountIdAndCurrentIsTrue(account.getId()))
                .get()
                .extracting(RepaymentSchedule::getRevisionNumber)
                .isEqualTo(3);
        assertThat(rowsOf(revision(account.getId(), 1))).hasSize(12);
        assertThat(rowsOf(revision(account.getId(), 2))).hasSize(12);
    }

    // ── the table the reissue persists is the one req signed ───────────────────

    @Nested
    @DisplayName("GD-miniloan-002 row 1 · read back out of the database")
    class TheReissuedTableIsTheSignedTable {

        /**
         * CALC-miniloan-001@v2 · GD-miniloan-002 row 1 (100,000 · 25% · 12 งวด) — <b>every expected
         * value here is that row copied character for character</b> (RQ24), asserted against the
         * instalments the reissue WROTE, not against what the calculator returned.
         */
        @Test
        void revisionTwoHoldsTheSignedRows() {
            LoanAccount account = disbursedAccount();

            Reissue reissued = reissueService.reissue(account.getId(), OPERATIONS);

            List<Installment> rows = rowsOf(reissued.schedule());
            assertThat(rows).hasSize(12);

            Installment first = rows.get(0);
            assertThat(Money.exact(first.getEmiAmount())).isEqualTo("9,504.42");
            assertThat(Money.exact(first.getInterestPortion())).isEqualTo("2,083.33");
            assertThat(Money.exact(first.getPrincipalPortion())).isEqualTo("7,421.09");

            Installment last = rows.get(rows.size() - 1);
            assertThat(Money.exact(last.getEmiAmount())).isEqualTo("9,504.43");
            assertThat(Money.exact(last.getInterestPortion())).isEqualTo("193.97");
            assertThat(Money.exact(last.getPrincipalPortion())).isEqualTo("9,310.46");

            // BR-miniloan-017@v1 holds of the REISSUED table too, not only of the first one.
            assertThat(Money.exact(last.getRemainingBalance())).isEqualTo("0.00");
            assertThat(Money.exact(reissued.schedule().getTotalPrincipal())).isEqualTo("100,000.00");
        }

        /**
         * BR-miniloan-037@v1: the account refers to the version it was opened under. A newer, cheaper
         * version published afterwards must not reach the reissued table — the rows would differ if
         * it did, and they are the signed 25% rows.
         */
        @Test
        void aRateVersionPublishedAfterDisbursementDoesNotReachTheReissue() {
            LoanAccount account = disbursedAccount();
            publish(new BigDecimal("10.0000"), LocalDate.now().minusDays(1));

            Reissue reissued = reissueService.reissue(account.getId(), OPERATIONS);

            assertThat(Money.exact(rowsOf(reissued.schedule()).get(0).getInterestPortion()))
                    .isEqualTo("2,083.33");
        }
    }

    // ── AC-miniloan-107 · 108 · 109 · BR-miniloan-045@v1 ───────────────────────

    /** AC-miniloan-107: a Closed account is refused, and its existing table is not touched. */
    @Test
    void aClosedAccountCannotBeReissued() {
        LoanAccount account = disbursedAccount();
        forceClosed(account.getId());

        assertThatThrownBy(() -> reissueService.reissue(account.getId(), OPERATIONS))
                .isInstanceOf(AccountClosedException.class)
                .hasMessage("บัญชีนี้ปิดแล้ว — ออกตารางผ่อนฉบับใหม่ทับไม่ได้");

        assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId())).hasSize(1);
        assertThat(rowsOf(revision(account.getId(), 1))).hasSize(12);
    }

    /**
     * AC-miniloan-109's boundary — the SAME account at two moments. Active: the reissue succeeds.
     * Closed: it does not, immediately, with no grace period between the two.
     */
    @Test
    void theRightToReissueDisappearsWithTheAccountClosing() {
        LoanAccount account = disbursedAccount();

        assertThat(reissueService.reissue(account.getId(), OPERATIONS).schedule().getRevisionNumber())
                .isEqualTo(2);

        forceClosed(account.getId());

        assertThatThrownBy(() -> reissueService.reissue(account.getId(), OPERATIONS))
                .isInstanceOf(AccountClosedException.class);
        assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId())).hasSize(2);
    }

    /**
     * AC-miniloan-108 — "ถึงมีผู้อนุมัติก็ออกฉบับใหม่ทับไม่ได้". No adjustment request exists in this
     * codebase yet (FE-miniloan-015/016 build that path), so what is measured is the property that
     * makes the criterion true for any approval that ever arrives: the entry point takes the account
     * and the caller and NOTHING ELSE, so there is no argument an approved adjustment could travel
     * in on, and the guard has no branch that could consult one.
     */
    @Test
    void noApprovalCanBeHandedToTheGuardBecauseThereIsNowhereToPutOne() throws Exception {
        assertThat(RepaymentScheduleReissueService.class.getMethod("reissue", UUID.class, String.class))
                .isNotNull();
        assertThat(RepaymentScheduleReissueService.class.getMethods())
                .filteredOn(method -> method.getName().equals("reissue"))
                .hasSize(1);

        LoanAccount account = disbursedAccount();
        forceClosed(account.getId());
        assertThatThrownBy(() -> reissueService.reissue(account.getId(), OPERATIONS))
                .isInstanceOf(AccountClosedException.class);
    }

    // ── ACL-009 · scope own ────────────────────────────────────────────────────

    /** ACL-009 is scope {@code own}: another Operations person may not reissue this account. */
    @Test
    void anotherOperationsPersonIsRefused() {
        LoanAccount account = disbursedAccount();

        assertThatThrownBy(() -> reissueService.reissue(account.getId(), ANOTHER_OPERATIONS))
                .isInstanceOf(NotAssignedOperationsException.class);

        assertThat(schedules.findByLoanAccountIdOrderByRevisionNumberAsc(account.getId())).hasSize(1);
    }
}

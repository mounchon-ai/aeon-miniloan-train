package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanApplication;
import com.miniloan.domain.Money;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.DisbursementService.Disbursement;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * เบิกจ่ายเงินกู้ (UC-miniloan-009 · BR-miniloan-014@v1 · BR-miniloan-015@v1 · BR-miniloan-036@v1 ·
 * BR-miniloan-037@v1).
 *
 * <p>The arithmetic itself is measured in {@link AmortizationScheduleServiceTest} against
 * GD-miniloan-002, so nothing here re-derives a baht figure. What is measured here is the wiring:
 * that the account and its table appear together, that the rate version chosen is the one in force
 * on the disbursement DATE, and that the account refers to that version rather than copying it.
 *
 * <p>The clock is a bean so a single day either side of an effective date can be measured
 * (AC-miniloan-101). It is the only thing this test replaces.
 */
@SpringBootTest
@Import(DisbursementServiceTest.SettableClockConfig.class)
class DisbursementServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ANOTHER_OFFICER = "STAFF-ข";

    /** AC-miniloan-100's dates, and the rate every row of GD-miniloan-002 was computed at. */
    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    private static final LocalDate OLD_VERSION_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate NEW_VERSION_FROM = LocalDate.of(2026, 9, 1);

    static final SettableClock CLOCK = new SettableClock();

    @TestConfiguration
    static class SettableClockConfig {
        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }

    /** A clock a test can move. Days matter here; the time of day never does. */
    static class SettableClock extends Clock {
        private volatile Instant now = LocalDate.of(2026, 8, 20).atStartOfDay(ZoneOffset.UTC).toInstant();

        void on(LocalDate date) {
            this.now = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

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
        CLOCK.on(LocalDate.of(2026, 8, 20));
    }

    private InterestRateVersion publish(LocalDate effectiveFrom) {
        return rateVersions.save(new InterestRateVersion(RATE_25_PERCENT, effectiveFrom, SUPERVISOR));
    }

    /** Income 30,000 → ceiling 150,000, so 100,000 over 12 instalments approves cleanly. */
    private UUID approved(BigDecimal amount, int termMonths, String officer) {
        UUID id =
                draftService
                        .saveNewDraft(
                                APPLICANT,
                                new DraftFields(
                                        "ทดสอบ ผู้สมัคร",
                                        35,
                                        new BigDecimal("30000.00"),
                                        24,
                                        new BigDecimal("1000.00"),
                                        amount,
                                        termMonths))
                        .getId();
        submitService.submit(id, APPLICANT);
        assignmentService.assign(id, officer, SUPERVISOR);
        approvalService.approve(id, null, officer);
        return id;
    }

    private UUID approved() {
        return approved(new BigDecimal("100000.00"), 12, OFFICER);
    }

    // ── the disbursement itself (BR-miniloan-014@v1 · BR-miniloan-015@v1) ───────

    /**
     * AC-miniloan-058 · AC-miniloan-093: the application moves, one Active account is opened, and
     * the table arrives WITH it — not as work queued for later.
     */
    @Test
    void disbursingOpensOneActiveAccountAndItsScheduleInTheSameBeat() {
        publish(OLD_VERSION_FROM);
        UUID id = approved();

        Disbursement result = disbursement.disburse(id, OFFICER);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Disbursed);
        assertThat(accounts.findAll()).hasSize(1);
        assertThat(result.account().getStatus()).isEqualTo(LoanAccount.Status.Active);
        assertThat(result.account().getPrincipalAmount()).isEqualByComparingTo("100000.00");
        assertThat(result.account().getOutstandingPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(result.account().getAssignedOperationsId()).isEqualTo(OPERATIONS);
        assertThat(result.installments()).hasSize(12);
        assertThat(result.schedule().getRevisionNumber()).isEqualTo(1);
        assertThat(result.schedule().isCurrent()).isTrue();
    }

    /**
     * AC-miniloan-094 — "ทันที" means the very next request, so this reads the table back through
     * the repositories rather than trusting the object the call returned.
     */
    @Test
    void theWholeTableIsThereOnTheNextRead() {
        publish(OLD_VERSION_FROM);
        UUID id = approved();

        Disbursement result = disbursement.disburse(id, OFFICER);

        var stored = schedules.findByLoanAccountIdAndCurrentIsTrue(result.account().getId());
        assertThat(stored).isPresent();
        List<Installment> rows =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(stored.orElseThrow().getId());
        assertThat(rows).hasSize(12);
        assertThat(rows).extracting(Installment::getStatus).containsOnly(Installment.Status.Due);
        // The figures are GD-miniloan-002 row 1, proven in AmortizationScheduleServiceTest; what is
        // checked here is that they survived the trip into the database unchanged.
        assertThat(Money.exact(rows.get(0).getEmiAmount())).isEqualTo("9,504.42");
        assertThat(Money.exact(rows.get(0).getInterestPortion())).isEqualTo("2,083.33");
        assertThat(Money.exact(rows.get(11).getRemainingBalance())).isEqualTo("0.00");
        assertThat(Money.exact(stored.orElseThrow().getTotalPrincipal())).isEqualTo("100,000.00");
    }

    /** AC-miniloan-095: before the disbursement there is no account and no table to open. */
    @Test
    void anApprovedApplicationHasNoScheduleUntilItIsDisbursed() {
        publish(OLD_VERSION_FROM);
        UUID id = approved();

        assertThat(accounts.findByApplicationId(id)).isEmpty();
        assertThat(disbursement.hasSchedule(id)).isFalse();
        assertThat(installments.findAll()).isEmpty();
    }

    /** AC-miniloan-059: not approved, so nothing is created and the application does not move. */
    @Test
    void anApplicationStillUnderReviewCannotBeDisbursed() {
        publish(OLD_VERSION_FROM);
        UUID id =
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
        submitService.submit(id, APPLICANT);
        assignmentService.assign(id, OFFICER, SUPERVISOR);

        assertThatThrownBy(() -> disbursement.disburse(id, OFFICER))
                .isInstanceOf(LoanApplication.NotDisbursableException.class)
                .hasMessage("เบิกจ่ายไม่ได้ — ใบสมัครนี้ยังไม่ได้รับอนุมัติ");

        assertThat(accounts.findAll()).isEmpty();
        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.UnderReview);
    }

    /**
     * AC-miniloan-060 · AC-miniloan-132 — the boundary of the one-to-one: pressing it again after a
     * failure that the user could not see the outcome of must not open a second account.
     */
    @Test
    void disbursingTwiceLeavesExactlyOneAccount() {
        publish(OLD_VERSION_FROM);
        UUID id = approved();
        disbursement.disburse(id, OFFICER);
        // The refusal quotes the row, and the column keeps microseconds where the in-memory Instant
        // still carries nanoseconds — so the expectation is read back, not taken from the object
        // the first call returned.
        LoanAccount stored = accounts.findByApplicationId(id).orElseThrow();

        assertThatThrownBy(() -> disbursement.disburse(id, OFFICER))
                .isInstanceOf(DisbursementService.AlreadyDisbursedException.class)
                .hasMessage("เบิกจ่ายไม่ได้ — ใบสมัครนี้เบิกจ่ายไปแล้วเมื่อ " + stored.getDisbursedAt());

        assertThat(accounts.findAll()).hasSize(1);
        assertThat(schedules.findAll()).hasSize(1);
        assertThat(installments.findAll()).hasSize(12);
    }

    /** BR-miniloan-032@v1 on this action too — ACL-008 is scope own, not "any Loan Officer". */
    @Test
    void anOfficerItWasNotAssignedToMayNotDisburse() {
        publish(OLD_VERSION_FROM);
        UUID id = approved(new BigDecimal("100000.00"), 12, ANOTHER_OFFICER);

        assertThatThrownBy(() -> disbursement.disburse(id, OFFICER))
                .isInstanceOf(LoanApplication.AssignedToAnotherOfficerException.class);

        assertThat(accounts.findAll()).isEmpty();
    }

    /** AC-miniloan-045: the whole main path, in order, with no step skipped. */
    @Test
    void theApplicationWalksTheWholeMainPathWithoutSkippingAStep() {
        publish(OLD_VERSION_FROM);
        UUID id =
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
        assertThat(applications.findById(id).orElseThrow().getStatus())
                .isEqualTo(LoanApplication.Status.Draft);

        submitService.submit(id, APPLICANT);
        assertThat(applications.findById(id).orElseThrow().getStatus())
                .isEqualTo(LoanApplication.Status.UnderReview);

        assignmentService.assign(id, OFFICER, SUPERVISOR);
        approvalService.approve(id, null, OFFICER);
        assertThat(applications.findById(id).orElseThrow().getStatus())
                .isEqualTo(LoanApplication.Status.Approved);

        disbursement.disburse(id, OFFICER);
        var finished = applications.findById(id).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(LoanApplication.Status.Disbursed);
        // Every move left an actor and a moment behind it.
        assertThat(finished.getSubmittedAt()).isNotNull();
        assertThat(finished.getApprovedBy()).isEqualTo(OFFICER);
        assertThat(finished.getApprovedAt()).isNotNull();
        assertThat(accounts.findByApplicationId(id).orElseThrow().getDisbursedAt()).isNotNull();
    }

    /** BR-miniloan-043@v1 · AC-miniloan-134: the claim is a row the database will refuse twice. */
    @Test
    void disbursingClaimsTheCommandKeyOnTheDatabase() {
        publish(OLD_VERSION_FROM);
        UUID id = approved();

        disbursement.disburse(id, OFFICER);

        assertThat(idempotencyKeys.findAll())
                .extracting(IdempotencyKey::getCommandType, IdempotencyKey::getRequestId)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(
                                IdempotencyKey.CommandType.DisburseLoan, id.toString()));
    }

    // ── which rate version applies (BR-miniloan-036@v1 · BR-miniloan-037@v1) ────

    /** AC-miniloan-100: two versions exist, and the one in force on the disbursement date wins. */
    @Test
    void theVersionInForceOnTheDisbursementDateIsTheOneUsed() {
        InterestRateVersion older = publish(OLD_VERSION_FROM);
        publish(NEW_VERSION_FROM);
        CLOCK.on(LocalDate.of(2026, 8, 20));
        UUID id = approved();

        Disbursement result = disbursement.disburse(id, OFFICER);

        assertThat(result.rateVersion().getId()).isEqualTo(older.getId());
        assertThat(result.rateVersion().getEffectiveFrom()).isEqualTo(OLD_VERSION_FROM);
    }

    /**
     * AC-miniloan-101 — the boundary, and it is one day wide: on the effective date itself the new
     * version applies, and the day before it does not. BR-miniloan-036@v1 says inclusive.
     */
    @Test
    void theEffectiveDateItselfBelongsToTheNewVersion() {
        InterestRateVersion older = publish(OLD_VERSION_FROM);
        InterestRateVersion newer = publish(NEW_VERSION_FROM);

        CLOCK.on(NEW_VERSION_FROM);
        UUID onTheDay = approved();
        assertThat(disbursement.disburse(onTheDay, OFFICER).rateVersion().getId()).isEqualTo(newer.getId());

        clean();
        publish(OLD_VERSION_FROM);
        publish(NEW_VERSION_FROM);
        CLOCK.on(NEW_VERSION_FROM.minusDays(1));
        UUID theDayBefore = approved();
        assertThat(disbursement.disburse(theDayBefore, OFFICER).rateVersion().getEffectiveFrom())
                .isEqualTo(older.getEffectiveFrom());
    }

    /** AC-miniloan-103: publishing a later version does not move one row of an existing table. */
    @Test
    void publishingALaterVersionLeavesAnExistingScheduleUntouched() {
        publish(OLD_VERSION_FROM);
        UUID id = approved();
        Disbursement result = disbursement.disburse(id, OFFICER);
        List<String> before = emiColumn(result);

        publish(LocalDate.now(CLOCK).plusDays(1));

        assertThat(emiColumn(result)).isEqualTo(before);
        assertThat(accounts.findByApplicationId(id).orElseThrow().getInterestRateVersionId())
                .isEqualTo(result.rateVersion().getId());
    }

    /**
     * AC-miniloan-104 · BR-miniloan-037@v1: the account keeps a REFERENCE. Two more versions land
     * afterwards and the account still resolves to the one its table was built with — which is only
     * possible because the number was never copied onto the account.
     */
    @Test
    void theAccountRefersToItsVersionRatherThanCopyingTheRate() {
        InterestRateVersion used = publish(OLD_VERSION_FROM);
        UUID id = approved();
        Disbursement result = disbursement.disburse(id, OFFICER);

        publish(LocalDate.now(CLOCK).plusDays(1));
        publish(LocalDate.now(CLOCK).plusDays(2));

        LoanAccount stored = accounts.findByApplicationId(id).orElseThrow();
        assertThat(stored.getInterestRateVersionId()).isEqualTo(used.getId());
        assertThat(rateVersions.findById(stored.getInterestRateVersionId()))
                .get()
                .extracting(InterestRateVersion::getEffectiveFrom)
                .isEqualTo(OLD_VERSION_FROM);
        assertThat(result.rateVersion().getAnnualRatePercent()).isEqualByComparingTo(RATE_25_PERCENT);
    }

    /**
     * AC-miniloan-106 — the moment the right to delete a version disappears is the moment the first
     * account binds to it, not the moment it was published. The refusal itself has no unit: no API
     * and no screen deletes a rate version, so what is measured here is the count that refusal would
     * quote, before and after.
     */
    @Test
    void aVersionHasNoAccountsUntilTheFirstDisbursementBindsOne() {
        InterestRateVersion version = publish(OLD_VERSION_FROM);
        assertThat(accounts.findByInterestRateVersionId(version.getId())).isEmpty();

        UUID id = approved();
        disbursement.disburse(id, OFFICER);

        assertThat(accounts.findByInterestRateVersionId(version.getId())).hasSize(1);
    }

    /** No version in force means no rate to build a table from, and refusing beats inventing one. */
    @Test
    void disbursingBeforeAnyVersionIsEffectiveIsRefused() {
        publish(NEW_VERSION_FROM);
        CLOCK.on(NEW_VERSION_FROM.minusDays(1));
        UUID id = approved();

        assertThatThrownBy(() -> disbursement.disburse(id, OFFICER))
                .isInstanceOf(DisbursementService.NoEffectiveRateException.class);

        assertThat(accounts.findAll()).isEmpty();
    }

    // ── due dates ──────────────────────────────────────────────────────────────

    /** Instalment t falls due t months after disbursement — BR-miniloan-004@v1 counts in months. */
    @Test
    void instalmentsFallDueOneMonthApartStartingAMonthAfterDisbursement() {
        publish(OLD_VERSION_FROM);
        CLOCK.on(LocalDate.of(2026, 8, 20));
        UUID id = approved();

        Disbursement result = disbursement.disburse(id, OFFICER);

        assertThat(result.installments().get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(result.installments().get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(result.installments().get(11).getDueDate()).isEqualTo(LocalDate.of(2027, 8, 20));
    }

    /**
     * Disbursing on a 31st: no rule covers a month that has no 31st, and {@code plusMonths} clamps
     * to the last day of the shorter month. Recorded here so the behaviour is a decision on the
     * record rather than something nobody looked at.
     */
    @Test
    void aMonthEndDisbursementClampsToTheLastDayOfShorterMonths() {
        publish(OLD_VERSION_FROM);
        CLOCK.on(LocalDate.of(2027, 1, 31));
        UUID id = approved();

        Disbursement result = disbursement.disburse(id, OFFICER);

        assertThat(result.installments().get(0).getDueDate()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(result.installments().get(1).getDueDate()).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    private List<String> emiColumn(Disbursement result) {
        var schedule = schedules.findByLoanAccountIdAndCurrentIsTrue(result.account().getId()).orElseThrow();
        return installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(schedule.getId()).stream()
                .map(row -> Money.exact(row.getEmiAmount()) + "/" + Money.exact(row.getRemainingBalance()))
                .toList();
    }
}

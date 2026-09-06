package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.RepaymentScheduleQueryService.AccountNotActiveException;
import com.miniloan.service.RepaymentScheduleQueryService.LoanAccountNotFoundException;
import com.miniloan.service.RepaymentScheduleQueryService.NotAccountOwnerException;
import com.miniloan.service.RepaymentScheduleQueryService.ScheduleView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ดูตารางผ่อนชำระ (UC-miniloan-011 · BR-miniloan-018@v1 · BR-miniloan-033@v1) — AC-miniloan-098 ·
 * AC-miniloan-099, at the domain layer ACL-010 names alongside the API.
 *
 * <p><b>Nothing is computed here.</b> The table this unit serves was produced by
 * CALC-miniloan-001@v2 and signed as GD-miniloan-002 when disbursement issued it (FE-miniloan-010),
 * so no expected instalment figure is invented in this file. What is measured is that the read hands
 * back the rows that are actually on disk — all of them, in order, with the status each one carries.
 *
 * <p><b>A second applicant, without a second token.</b> FE-miniloan-002's mock scheme mints one
 * identity per role, so there is no second Applicant token to log in as. The identity that reaches
 * the service is a plain {@code String}, and {@link LoanApplicationDraftService#saveNewDraft} takes
 * it as an argument — so AC-miniloan-099's "ผู้สมัคร ข." is expressible as an application owned by a
 * different applicant id, and the guard is measured against a real stranger rather than a mocked
 * one. {@link MockTokenService} is not touched.
 *
 * <p><b>Closing an account is arranged, not driven</b> — the same note
 * {@code AmortizationScheduleReissueTest} carries. {@link LoanAccount} exposes no status mutator and
 * FE-miniloan-013 is the unit that closes one, so the Closed state is written straight onto the row
 * through JPQL and ACL-010's condition is measured against it.
 */
@SpringBootTest
class RepaymentScheduleQueryServiceTest {

    private static final String APPLICANT = "ROLE-001";

    /** AC-miniloan-099's "ผู้สมัคร ข." — a different person, not a different role. */
    private static final String ANOTHER_APPLICANT = "ROLE-001-another-person";

    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";

    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    @Autowired private RepaymentScheduleQueryService queryService;
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

    /** AC-miniloan-008's account exactly — 100,000 บาท · 12 งวด · 25% ต่อปี (GD-miniloan-002 row 1). */
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

    // ── AC-miniloan-098 · the whole table, with a status on every row ──────────

    /**
     * "ทุกงวดตั้งแต่งวดที่ 1 ถึงงวดสุดท้าย ไม่ใช่เฉพาะงวดที่ยังไม่ถึงกำหนด" — twelve rows, numbered 1
     * to 12, in order, and the figures are the ones disbursement wrote, read back off the database.
     */
    @Test
    void theOwnerSeesEveryInstalmentFromTheFirstToTheLast() {
        LoanAccount account = disbursedAccountOwnedBy(APPLICANT);

        ScheduleView view = queryService.view(account.getId(), APPLICANT);

        assertThat(view.installments()).hasSize(12);
        assertThat(view.installments())
                .extracting(Installment::getInstallmentNumber)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        var onDisk =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(
                        view.schedule().getId());
        assertThat(view.installments())
                .extracting(Installment::getId)
                .containsExactlyElementsOf(onDisk.stream().map(Installment::getId).toList());
    }

    /**
     * "แต่ละงวดมีป้ายสถานะ … กำกับชัดเจน" — every row carries a state from STM-miniloan-003, none is
     * null, and the row that was paid is distinguishable from the ones still due. The Thai labels
     * AC-miniloan-098 quotes are UI-miniloan-004's rendering of these; this unit reaches no screen.
     */
    @Test
    void everyRowCarriesItsOwnStatusAndAPaidRowIsDistinguishable() {
        LoanAccount account = disbursedAccountOwnedBy(APPLICANT);
        markPaid(account.getId(), 1);

        ScheduleView view = queryService.view(account.getId(), APPLICANT);

        assertThat(view.installments()).allSatisfy(row -> assertThat(row.getStatus()).isNotNull());
        assertThat(view.installments().get(0).getStatus()).isEqualTo(Installment.Status.Paid);
        assertThat(view.installments().subList(1, 12))
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(Installment.Status.Due));
    }

    /**
     * The instalment already settled is still a row of the table — BR-miniloan-018@v1's whole point,
     * and the thing a "show me what is left" read would quietly drop.
     */
    @Test
    void aPaidInstalmentIsStillListed() {
        LoanAccount account = disbursedAccountOwnedBy(APPLICANT);
        markPaid(account.getId(), 1);

        ScheduleView view = queryService.view(account.getId(), APPLICANT);

        assertThat(view.installments()).hasSize(12);
        assertThat(view.installments())
                .filteredOn(row -> row.getStatus() == Installment.Status.Paid)
                .extracting(Installment::getInstallmentNumber)
                .containsExactly(1);
    }

    /**
     * "เห็นเงินต้น ดอกเบี้ย และยอดคงเหลือของแต่ละงวด" — all three are present on every row, and
     * UC-miniloan-011's alternate flow reads the closing balance off the last one: BR-miniloan-017@v1
     * puts it at exactly zero, and the principal of the twelve rows sums to the stored
     * {@code totalPrincipal}.
     */
    @Test
    void everyRowCarriesPrincipalInterestAndBalanceAndTheTableClosesAtZero() {
        LoanAccount account = disbursedAccountOwnedBy(APPLICANT);

        ScheduleView view = queryService.view(account.getId(), APPLICANT);

        assertThat(view.installments())
                .allSatisfy(
                        row -> {
                            assertThat(row.getPrincipalPortion()).isNotNull();
                            assertThat(row.getInterestPortion()).isNotNull();
                            assertThat(row.getRemainingBalance()).isNotNull();
                            assertThat(row.getEmiAmount()).isNotNull();
                        });

        assertThat(view.installments().get(11).getRemainingBalance()).isEqualByComparingTo("0.00");
        assertThat(
                        view.installments().stream()
                                .map(Installment::getPrincipalPortion)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(view.schedule().getTotalPrincipal());
    }

    /**
     * API-012 says ปัจจุบัน: after FE-miniloan-011 issues revision 2 over the top, the read returns
     * revision 2's rows — not revision 1's, and not both sets mixed together.
     */
    @Test
    void theReadReturnsTheRevisionInForce() {
        LoanAccount account = disbursedAccountOwnedBy(APPLICANT);
        var reissued = reissueService.reissue(account.getId(), account.getAssignedOperationsId());

        ScheduleView view = queryService.view(account.getId(), APPLICANT);

        assertThat(view.schedule().getRevisionNumber()).isEqualTo(2);
        assertThat(view.schedule().getId()).isEqualTo(reissued.schedule().getId());
        assertThat(view.installments())
                .extracting(Installment::getRepaymentScheduleId)
                .containsOnly(reissued.schedule().getId());
    }

    // ── AC-miniloan-099 · BR-miniloan-033@v1 · nobody else's table ─────────────

    /**
     * ก. asks for ข.'s account. The refusal carries the sentence the criterion quotes, and — the half
     * that matters — the exception carries no rows: there is no partial answer to leak from.
     */
    @Test
    void anotherApplicantsScheduleIsRefusedAndNoRowEscapes() {
        LoanAccount theirs = disbursedAccountOwnedBy(ANOTHER_APPLICANT);

        assertThatThrownBy(() -> queryService.view(theirs.getId(), APPLICANT))
                .isInstanceOf(NotAccountOwnerException.class)
                .hasMessage("ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้");

        // and their table is untouched and still whole, so the refusal read nothing away.
        var current = schedules.findByLoanAccountIdAndCurrentIsTrue(theirs.getId()).orElseThrow();
        assertThat(installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId()))
                .hasSize(12);
    }

    /**
     * The owner check runs BEFORE the state check, so a stranger cannot learn from the refusal
     * whether the account they guessed at is Active or Closed.
     */
    @Test
    void aStrangerGetsTheSameRefusalWhetherTheAccountIsActiveOrClosed() {
        LoanAccount active = disbursedAccountOwnedBy(ANOTHER_APPLICANT);

        assertThatThrownBy(() -> queryService.view(active.getId(), APPLICANT))
                .isInstanceOf(NotAccountOwnerException.class);

        forceClosed(active.getId());

        assertThatThrownBy(() -> queryService.view(active.getId(), APPLICANT))
                .isInstanceOf(NotAccountOwnerException.class);
    }

    // ── ACL-010's condition and the account that is not there ──────────────────

    /** UC-miniloan-011's precondition · ACL-010 {@code state: [Active]}. */
    @Test
    void aClosedAccountIsRefusedEvenToItsOwner() {
        LoanAccount account = disbursedAccountOwnedBy(APPLICANT);
        forceClosed(account.getId());

        assertThatThrownBy(() -> queryService.view(account.getId(), APPLICANT))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void anUnknownAccountIsANotFound() {
        UUID missing = UUID.randomUUID();

        assertThatThrownBy(() -> queryService.view(missing, APPLICANT))
                .isInstanceOf(LoanAccountNotFoundException.class);
    }

    /**
     * FE-miniloan-013 is the unit that records a payment; until it lands, a Paid row is arranged the
     * same way a Closed account is — written onto the row, not driven through a mutator this unit
     * would have to invent.
     */
    private void markPaid(UUID accountId, int installmentNumber) {
        var current = schedules.findByLoanAccountIdAndCurrentIsTrue(accountId).orElseThrow();
        List<Installment> rows =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId());
        UUID rowId = rows.get(installmentNumber - 1).getId();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                entityManager
                                        .createQuery("update Installment i set i.status = :paid where i.id = :id")
                                        .setParameter("paid", Installment.Status.Paid)
                                        .setParameter("id", rowId)
                                        .executeUpdate());
    }
}

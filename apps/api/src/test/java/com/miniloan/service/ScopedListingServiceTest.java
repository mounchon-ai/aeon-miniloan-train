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
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.ScopedListingService.ApplicationNotVisibleException;
import com.miniloan.service.ScopedListingService.ListingNotPermittedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ขอบเขตข้อมูลของ Applicant (UC-miniloan-023 · BR-miniloan-033@v1) — AC-miniloan-126 ·
 * AC-miniloan-127 · AC-miniloan-128, at the domain layer ACL-019 names alongside the API.
 *
 * <p><b>The list is the route that matters, and the count is half the measure.</b> AC-miniloan-128
 * says a scope hole lives in the list rather than in the single read, so every assertion here is
 * made with another applicant's rows sitting in the same table, and the size is asserted as well as
 * the contents: a list that merely contains the caller's own applications also passes when it
 * contains everybody else's.
 *
 * <p><b>A second applicant, without a second token.</b> FE-miniloan-002's mock scheme mints one
 * identity per role, so there is no second Applicant to log in as. The identity that reaches the
 * service is a plain {@code String} and {@link LoanApplicationDraftService#saveNewDraft} takes it as
 * an argument, so AC-miniloan-127's "ผู้สมัคร ข." is expressible as applications owned by a
 * different applicant id — the same arrangement {@code RepaymentScheduleQueryServiceTest} uses.
 *
 * <p><b>The account half of AC-miniloan-126 is measured against the route that already serves it.</b>
 * {@link LoanAccountScopeService} was built with API-013's scope in it and its own note says this
 * unit extends that surface rather than adding a second one; what is proved here is that the
 * criterion's "1 บัญชีของตัวเอง" really comes back, with a stranger's account in the table.
 */
@SpringBootTest
class ScopedListingServiceTest {

    private static final String APPLICANT = "ROLE-001";

    /** AC-miniloan-127's "ผู้สมัคร ข." — a different person, not a different role. */
    private static final String ANOTHER_APPLICANT = "ROLE-001-another-person";

    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ADJUSTMENT_APPROVER = "ROLE-005";

    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("25.0000");

    @Autowired private ScopedListingService listing;
    @Autowired private LoanAccountScopeService accountScope;
    @Autowired private LoanApplicationSubmitService submitService;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private ApplicationAssignmentService assignmentService;
    @Autowired private LoanApplicationApprovalService approvalService;
    @Autowired private DisbursementService disbursement;
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

    // ── AC-miniloan-126 · what the owner sees ─────────────────────────────────

    /**
     * "ผู้สมัคร ก. ที่มีใบสมัคร 2 ใบและบัญชีสินเชื่อ 1 บัญชีเป็นของตัวเอง … เห็นครบทั้ง 2 ใบและ 1
     * บัญชีของตัวเองตามปกติ" — the scope is not only a fence, it must also let the owner's own rows
     * through. A stranger's application and account are in the table throughout.
     */
    @Test
    void theOwnerSeesTheirOwnTwoApplicationsAndOneAccount() {
        LoanAccount ownAccount = disbursedAccountOwnedBy(APPLICANT);
        draftFor(APPLICANT);
        disbursedAccountOwnedBy(ANOTHER_APPLICANT);

        List<LoanApplication> visible = listing.applicationsVisibleTo(APPLICANT);
        List<LoanAccount> visibleAccounts = accountScope.visibleTo(APPLICANT);

        assertThat(visible).hasSize(2);
        assertThat(visible).allSatisfy(row -> assertThat(row.getApplicantId()).isEqualTo(APPLICANT));
        assertThat(visibleAccounts).hasSize(1);
        assertThat(visibleAccounts.get(0).getId()).isEqualTo(ownAccount.getId());
    }

    // ── AC-miniloan-128 · the list is where the hole lives ────────────────────

    /**
     * "ไม่มีใบของคนอื่นหลุดเข้ามาแม้แต่ใบเดียว และจำนวนรายการที่ได้ตรงกับจำนวนใบของ ก. เท่านั้น" —
     * two of ก.'s applications against three of ข.'s in one table. The size is asserted because a
     * list that returned all five would still "contain ก.'s" and would pass a weaker check.
     */
    @Test
    void aListWithSeveralOwnersLetsThroughExactlyTheCallersOwnRows() {
        UUID mineFirst = draftFor(APPLICANT);
        UUID mineSecond = draftFor(APPLICANT);
        UUID theirsFirst = draftFor(ANOTHER_APPLICANT);
        UUID theirsSecond = draftFor(ANOTHER_APPLICANT);
        UUID theirsThird = draftFor(ANOTHER_APPLICANT);

        assertThat(applications.count()).isEqualTo(5);

        List<UUID> visible = listing.applicationsVisibleTo(APPLICANT).stream().map(LoanApplication::getId).toList();

        assertThat(visible).hasSize(2);
        assertThat(visible).containsExactly(mineFirst, mineSecond);
        assertThat(visible).doesNotContain(theirsFirst, theirsSecond, theirsThird);
    }

    // ── AC-miniloan-127 · refused at the API, and told nothing ────────────────

    /**
     * "ก. เรียก API เปิดดูใบสมัครของ ข. ด้วย id ของ ข. โดยตรง ไม่ผ่านหน้าจอ … API ปฏิเสธ —
     * ไม่มีสิทธิ์เข้าถึงใบสมัครนี้". The id is real and the row exists; what is missing is the right
     * to see it.
     */
    @Test
    void theOwnerOfAnotherApplicationIsRefusedByTheServiceItself() {
        UUID theirs = draftFor(ANOTHER_APPLICANT);

        assertThatThrownBy(() -> submitService.findDetail(theirs, APPLICANT))
                .isInstanceOf(ApplicationNotVisibleException.class)
                .hasMessage("ไม่มีสิทธิ์เข้าถึงใบสมัครนี้");
    }

    /**
     * The same sentence for an id that belongs to nobody. Telling "not yours" apart from "not there"
     * would turn the refusal into a way of learning which application ids exist, which is what
     * BR-miniloan-033@v1 is there to stop — the reasoning {@code LoanAccountScopeService} already
     * records on the account side.
     */
    @Test
    void anIdThatBelongsToNobodyGetsTheSameSentence() {
        UUID theirs = draftFor(ANOTHER_APPLICANT);
        UUID nobodys = UUID.randomUUID();

        Throwable forAStranger = catchRefusal(theirs);
        Throwable forNothing = catchRefusal(nobodys);

        assertThat(forAStranger).isInstanceOf(ApplicationNotVisibleException.class);
        assertThat(forNothing).isInstanceOf(ApplicationNotVisibleException.class);
        assertThat(forNothing.getMessage()).isEqualTo(forAStranger.getMessage());
    }

    // ── the other declared scopes ─────────────────────────────────────────────

    /** ACL-003 · ACL-031 give ROLE-003 scope: all, and the list agrees with what findDetail allows. */
    @Test
    void theSupervisorSeesEveryApplication() {
        draftFor(APPLICANT);
        draftFor(ANOTHER_APPLICANT);
        draftFor(ANOTHER_APPLICANT);

        assertThat(listing.applicationsVisibleTo(SUPERVISOR)).hasSize(3);
    }

    /**
     * Default-deny. ACL-027 gives ROLE-002 scope=own over UI-miniloan-006's queue, but that is the
     * officer's assigned work and no unit has built its API surface — {@code findDetail} refuses the
     * same role for the same reason. ROLE-004 and ROLE-005 have no application row at all.
     */
    @Test
    void aRoleWithNoDeclaredApplicationListIsRefused() {
        draftFor(APPLICANT);

        for (String role : new String[] {OFFICER, OPERATIONS, ADJUSTMENT_APPROVER}) {
            assertThatThrownBy(() -> listing.applicationsVisibleTo(role))
                    .isInstanceOf(ListingNotPermittedException.class);
        }
    }

    // ── arrangement ──────────────────────────────────────────────────────────

    private Throwable catchRefusal(UUID id) {
        try {
            submitService.findDetail(id, APPLICANT);
            throw new AssertionError("expected a refusal for " + id);
        } catch (RuntimeException refused) {
            return refused;
        }
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

    /** AC-miniloan-126's "1 บัญชีสินเชื่อ" — reached through the real flow, not written into the table. */
    private LoanAccount disbursedAccountOwnedBy(String applicantId) {
        if (rateVersions.count() == 0) {
            rateVersions.save(
                    new InterestRateVersion(RATE_25_PERCENT, LocalDate.now().minusYears(1), SUPERVISOR));
        }
        UUID applicationId = draftFor(applicantId);
        submitService.submit(applicationId, applicantId);
        assignmentService.assign(applicationId, OFFICER, SUPERVISOR);
        approvalService.approve(applicationId, null, OFFICER);
        return disbursement.disburse(applicationId, OFFICER).account();
    }
}

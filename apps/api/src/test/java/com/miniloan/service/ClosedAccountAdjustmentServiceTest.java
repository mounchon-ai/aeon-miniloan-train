package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.domain.LoanAccount;
import com.miniloan.repository.ApproverRoleSettingRepository;
import com.miniloan.repository.ClosedAccountAdjustmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.service.ClosedAccountAdjustmentService.AccountNotClosedException;
import com.miniloan.service.ClosedAccountAdjustmentService.AdjustmentFields;
import com.miniloan.service.ClosedAccountAdjustmentService.AdjustmentFieldsRequiredException;
import com.miniloan.service.ClosedAccountAdjustmentService.ApproverRoleNotSetException;
import com.miniloan.service.ClosedAccountAdjustmentService.DirectAccountEditRefusedException;
import com.miniloan.service.ClosedAccountAdjustmentService.NotAssignedOperationsException;
import com.miniloan.service.ClosedAccountAdjustmentService.OperationsOnlyException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FE-miniloan-015 · UC-miniloan-017 · BR-miniloan-038@v1 · BR-miniloan-040@v1 · BR-miniloan-041@v1.
 *
 * <p>The mock-token scheme resolves one identity per role (FE-miniloan-002), so a caller here IS a
 * role string — the same reading every unit since FE-miniloan-010 works under, and the reason
 * {@code requestedBy} is asserted as {@code ROLE-004}. {@code ANOTHER_OPERATIONS} is how
 * ACL-015's {@code scope: own} is exercised among people holding the very same role, exactly as
 * {@code PaymentControllerTest} does it.
 *
 * <p>AC-miniloan-076's second half — the approval, and the account taking the new value — is
 * FE-miniloan-016's, and AC-miniloan-079's sentence with it. What is proved here is the half this
 * unit owns: the request is filed, it waits, and the account does not move.
 */
@SpringBootTest
class ClosedAccountAdjustmentServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String LOAN_OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ANOTHER_OPERATIONS = "ROLE-004-another-person";
    private static final String ADJUSTMENT_APPROVER = "ROLE-005";

    private static final AdjustmentFields LAST_PAYMENT =
            new AdjustmentFields("lastPaymentAmount", "8,500.00", "8,050.00");

    @Autowired private ClosedAccountAdjustmentService service;
    @Autowired private ApproverRoleSettingService approverSettings;
    @Autowired private RepaymentScheduleReissueService reissueService;
    @Autowired private ClosedAccountAdjustmentRepository adjustments;
    @Autowired private ApproverRoleSettingRepository approverRoles;
    @Autowired private LoanAccountRepository accounts;

    @BeforeEach
    void clean() {
        adjustments.deleteAll();
        approverRoles.deleteAll();
    }

    /** BR-miniloan-039@v1 has to be answered before this feature does anything at all. */
    private void approverRoleIsSetTo(ApproverRole role) {
        approverSettings.set(LOAN_OFFICER, role);
    }

    private LoanAccount closedAccountOf(String operations) {
        LoanAccount account = activeAccountOf(operations);
        account.reducePrincipal(account.getOutstandingPrincipal());
        account.close(LoanAccount.CloseReason.FullyPaid, Instant.now());
        return accounts.save(account);
    }

    private LoanAccount activeAccountOf(String operations) {
        return accounts.save(
                new LoanAccount(
                        UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000.00"), 12, operations));
    }

    @Nested
    class FilingARequest {

        /** AC-miniloan-076's first half, and BR-miniloan-041@v1: a row of its own, waiting. */
        @Test
        void theRequestIsFiledAndWaits() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = closedAccountOf(OPERATIONS);

            var result = service.submit(account.getId(), OPERATIONS, LAST_PAYMENT);

            assertThat(result.message())
                    .isEqualTo("ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก LoanOfficer");
            ClosedAccountAdjustment filed = result.adjustment();
            assertThat(filed.getStatus()).isEqualTo(ClosedAccountAdjustment.Status.Pending);
            assertThat(filed.getLoanAccountId()).isEqualTo(account.getId());
            assertThat(filed.getFieldName()).isEqualTo("lastPaymentAmount");
            assertThat(filed.getOldValue()).isEqualTo("8,500.00");
            assertThat(filed.getNewValue()).isEqualTo("8,050.00");
            assertThat(filed.getRequestedBy()).isEqualTo(OPERATIONS);
            assertThat(filed.getRequestedAt()).isNotNull();
        }

        /**
         * AC-miniloan-085's shape at the moment of filing: no approver and no approval time, and no
         * placeholder standing in for either. BR-miniloan-041@v1 says a real adjustment needs both.
         */
        @Test
        void anUndecidedRequestHasNoApproverAndNoApprovalTime() {
            approverRoleIsSetTo(ApproverRole.Supervisor);
            LoanAccount account = closedAccountOf(OPERATIONS);

            ClosedAccountAdjustment filed =
                    service.submit(account.getId(), OPERATIONS, LAST_PAYMENT).adjustment();

            assertThat(filed.getApprovedBy()).isNull();
            assertThat(filed.getApprovedAt()).isNull();
        }

        /**
         * BR-miniloan-041@v1 · BR-miniloan-038@v1 — "ค่าเดิมของบัญชีคงไว้ไม่ถูกทับ". The account is
         * re-read from the database rather than from the object the test still holds, because an
         * in-memory copy would agree with itself.
         */
        @Test
        void filingTouchesNothingOnTheAccount() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = closedAccountOf(OPERATIONS);
            Instant closedAt = account.getClosedAt();

            service.submit(account.getId(), OPERATIONS, LAST_PAYMENT);

            LoanAccount reloaded = accounts.findById(account.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(LoanAccount.Status.Closed);
            assertThat(reloaded.getCloseReason()).isEqualTo(LoanAccount.CloseReason.FullyPaid);
            // H2 stores an Instant at microsecond precision and ROUNDS on the way in, so a
            // nanosecond-exact comparison measures the storage engine rather than the rule. A
            // closedAt that had actually moved would be off by far more than a millisecond.
            assertThat(reloaded.getClosedAt()).isCloseTo(closedAt, within(1, ChronoUnit.MILLIS));
            assertThat(reloaded.getPrincipalAmount()).isEqualByComparingTo("100000.00");
            assertThat(reloaded.getOutstandingPrincipal()).isEqualByComparingTo("0.00");
        }

        /**
         * The waiting role in the message is READ from the setting, not written into this unit —
         * which is what makes BR-miniloan-039@v1's "เปลี่ยนได้ภายหลังโดยไม่ต้องแก้โปรแกรม" true on
         * this side of it as well.
         */
        @Test
        void theRoleItWaitsOnComesFromTheSetting() {
            approverRoleIsSetTo(ApproverRole.Supervisor);
            LoanAccount first = closedAccountOf(OPERATIONS);
            assertThat(service.submit(first.getId(), OPERATIONS, LAST_PAYMENT).message())
                    .isEqualTo("ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก Supervisor");

            approverRoleIsSetTo(ApproverRole.Operations);

            LoanAccount second = closedAccountOf(OPERATIONS);
            assertThat(service.submit(second.getId(), OPERATIONS, LAST_PAYMENT).message())
                    .isEqualTo("ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก Operations");
        }
    }

    @Nested
    class WithNoApproverRoleSet {

        /** AC-miniloan-080, word for word — and nothing is created. */
        @Test
        void filingIsRefusedAndNoRequestExists() {
            LoanAccount account = closedAccountOf(OPERATIONS);

            assertThatThrownBy(() -> service.submit(account.getId(), OPERATIONS, LAST_PAYMENT))
                    .isInstanceOf(ApproverRoleNotSetException.class)
                    .hasMessage(
                            "ยังไม่ได้ตั้ง role ผู้อนุมัติ — ใช้ฟีเจอร์แก้ข้อมูลบัญชีที่ปิดแล้วไม่ได้ · ให้ Loan Officer ตั้งค่า role ผู้อนุมัติก่อน");

            assertThat(adjustments.findByLoanAccountIdOrderByRequestedAtAsc(account.getId())).isEmpty();
            assertThat(adjustments.count()).isZero();
        }

        /**
         * BR-miniloan-040@v1 says "ทุกกรณี", so the gate is on the FEATURE and not on one account:
         * an id that names no account at all is refused for the same reason rather than with a 404,
         * which is also why no caller can probe for accounts through this route while it is shut.
         */
        @Test
        void theGateIsOnTheFeatureNotOnOneAccount() {
            assertThatThrownBy(() -> service.submit(UUID.randomUUID(), OPERATIONS, LAST_PAYMENT))
                    .isInstanceOf(ApproverRoleNotSetException.class);

            assertThat(adjustments.count()).isZero();
        }

        /** AC-miniloan-081, word for word — the direct edit is refused with the "no approver" reason. */
        @Test
        void aDirectAccountEditIsRefusedWithTheNoApproverReason() {
            assertThatThrownBy(() -> service.refuseDirectAccountEdit())
                    .isInstanceOf(DirectAccountEditRefusedException.class)
                    .hasMessage("ยังไม่ได้ตั้ง role ผู้อนุมัติ — การแก้ไขมีผลก่อนได้รับอนุมัติไม่ได้ทุกกรณี");

            assertThat(adjustments.count()).isZero();
        }
    }

    @Nested
    class WhoMayFileIt {

        /** ACL-015 names ROLE-004 alone, and rbac.json is default-deny. */
        @Test
        void everyOtherRoleIsRefused() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = closedAccountOf(OPERATIONS);

            for (String role :
                    new String[] {APPLICANT, LOAN_OFFICER, SUPERVISOR, ADJUSTMENT_APPROVER, null}) {
                assertThatThrownBy(() -> service.submit(account.getId(), role, LAST_PAYMENT))
                        .isInstanceOf(OperationsOnlyException.class)
                        .hasMessage("ไม่มีสิทธิ์ยื่นคำขอปรับปรุงบัญชีที่ปิดแล้ว — ทำได้เฉพาะเจ้าหน้าที่ Operations");
            }

            assertThat(adjustments.count()).isZero();
        }

        /** ACL-015's {@code scope: own} · BR-miniloan-054@v1, among Operations holding the same role. */
        @Test
        void anotherOperationsPersonIsRefused() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = closedAccountOf(ANOTHER_OPERATIONS);

            assertThatThrownBy(() -> service.submit(account.getId(), OPERATIONS, LAST_PAYMENT))
                    .isInstanceOf(NotAssignedOperationsException.class);

            assertThat(adjustments.count()).isZero();
        }

        /** ACL-015's condition is {@code STM-miniloan-002} state Closed — this is the after-close path. */
        @Test
        void anAccountStillActiveIsRefused() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = activeAccountOf(OPERATIONS);

            assertThatThrownBy(() -> service.submit(account.getId(), OPERATIONS, LAST_PAYMENT))
                    .isInstanceOf(AccountNotClosedException.class)
                    .hasMessage("บัญชีนี้ยังไม่ปิด — คำขอปรับปรุงใช้กับบัญชีที่ปิดแล้วเท่านั้น");

            assertThat(adjustments.count()).isZero();
        }

        /** ENT-010 declares all three required, so a blank is refused rather than stored as one. */
        @Test
        void anIncompleteRequestIsRefused() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = closedAccountOf(OPERATIONS);

            assertThatThrownBy(
                            () ->
                                    service.submit(
                                            account.getId(), OPERATIONS, new AdjustmentFields("lastPaymentAmount", " ", "8,050.00")))
                    .isInstanceOf(AdjustmentFieldsRequiredException.class);
            assertThatThrownBy(
                            () ->
                                    service.submit(
                                            account.getId(), OPERATIONS, new AdjustmentFields(null, "8,500.00", "8,050.00")))
                    .isInstanceOf(AdjustmentFieldsRequiredException.class);

            assertThat(adjustments.count()).isZero();
        }
    }

    @Nested
    class TheRequestsOwnLifeCycle {

        /** STM-miniloan-004's Pending→Approved, with the five fields BR-miniloan-041@v1 asks for. */
        @Test
        void approvingItRecordsWhoAndWhen() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = closedAccountOf(OPERATIONS);
            ClosedAccountAdjustment filed =
                    service.submit(account.getId(), OPERATIONS, LAST_PAYMENT).adjustment();
            Instant decidedAt = Instant.now();

            filed.approve(LOAN_OFFICER, decidedAt);

            assertThat(filed.getStatus()).isEqualTo(ClosedAccountAdjustment.Status.Approved);
            assertThat(filed.getApprovedBy()).isEqualTo(LOAN_OFFICER);
            assertThat(filed.getApprovedAt()).isEqualTo(decidedAt);
            assertThat(filed.getOldValue()).isEqualTo("8,500.00");
        }

        /**
         * BR-miniloan-049@v1 at the layer that owns the invariant — the floor under AC-miniloan-079,
         * whose sentence FE-miniloan-016 says at the route before a caller ever reaches this object.
         */
        @Test
        void theRequesterCannotDecideTheirOwnRequest() {
            approverRoleIsSetTo(ApproverRole.Operations);
            LoanAccount account = closedAccountOf(OPERATIONS);
            ClosedAccountAdjustment filed =
                    service.submit(account.getId(), OPERATIONS, LAST_PAYMENT).adjustment();

            assertThatThrownBy(() -> filed.approve(OPERATIONS, Instant.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้");
            assertThatThrownBy(() -> filed.reject(OPERATIONS, Instant.now()))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(filed.getStatus()).isEqualTo(ClosedAccountAdjustment.Status.Pending);
        }

        /** STM-miniloan-004: Approved and Rejected are both final. */
        @Test
        void aDecidedRequestCannotBeDecidedAgain() {
            approverRoleIsSetTo(ApproverRole.LoanOfficer);
            LoanAccount account = closedAccountOf(OPERATIONS);
            ClosedAccountAdjustment filed =
                    service.submit(account.getId(), OPERATIONS, LAST_PAYMENT).adjustment();
            filed.reject(LOAN_OFFICER, Instant.now());

            assertThatThrownBy(() -> filed.approve(SUPERVISOR, Instant.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("คำขอปรับปรุงนี้ถูกพิจารณาไปแล้ว — พิจารณาซ้ำไม่ได้");

            assertThat(filed.getStatus()).isEqualTo(ClosedAccountAdjustment.Status.Rejected);
        }
    }

    /**
     * AC-miniloan-108 · BR-miniloan-045@v1. FE-miniloan-011 already refuses a reissue on a Closed
     * account and this unit does not touch that service; what only this can add is the case the
     * criterion is actually about — an adjustment that has been APPROVED, which is the thing a reader
     * might expect to unlock the schedule. It does not.
     */
    @Test
    void anApprovedAdjustmentStillDoesNotUnlockTheSchedule() {
        approverRoleIsSetTo(ApproverRole.LoanOfficer);
        LoanAccount account = closedAccountOf(OPERATIONS);
        ClosedAccountAdjustment filed =
                service.submit(account.getId(), OPERATIONS, LAST_PAYMENT).adjustment();
        filed.approve(LOAN_OFFICER, Instant.now());
        adjustments.save(filed);

        assertThatThrownBy(() -> reissueService.reissue(account.getId(), OPERATIONS))
                .isInstanceOf(RepaymentScheduleReissueService.AccountClosedException.class)
                .hasMessage("บัญชีนี้ปิดแล้ว — ออกตารางผ่อนฉบับใหม่ทับไม่ได้");

        assertThat(accounts.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanAccount.Status.Closed);
        assertThat(adjustments.findById(filed.getId()).orElseThrow().getStatus())
                .isEqualTo(ClosedAccountAdjustment.Status.Approved);
    }

    /** AC-miniloan-077's sentence — the refusal a caller meets once an approver HAS been named. */
    @Test
    void aDirectAccountEditIsRefusedWithTheUseTheRequestReason() {
        approverRoleIsSetTo(ApproverRole.LoanOfficer);

        assertThatThrownBy(() -> service.refuseDirectAccountEdit())
                .isInstanceOf(DirectAccountEditRefusedException.class)
                .hasMessage("แก้ข้อมูลบัญชีที่ปิดแล้วโดยตรงไม่ได้ — ต้องยื่นคำขอปรับปรุงและรออนุมัติก่อน");
    }
}

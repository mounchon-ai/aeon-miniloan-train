package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.domain.ClosedAccountAdjustment.AdjustableField;
import com.miniloan.domain.ClosedAccountAdjustment.Status;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.Payment;
import com.miniloan.repository.ApproverRoleSettingRepository;
import com.miniloan.repository.ClosedAccountAdjustmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.PaymentRepository;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.AdjustedValueUnreadableException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.AdjustmentAlreadyDecidedException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.AdjustmentNotFoundException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.DecisionResult;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.NotTheApproverException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.SelfApprovalRefusedException;
import com.miniloan.service.ClosedAccountAdjustmentService.ApproverRoleNotSetException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FE-miniloan-016 · UC-miniloan-018 · AC-miniloan-076 · AC-miniloan-079 · ACL-016.
 *
 * <p>The mock-token scheme resolves one identity per role (FE-miniloan-002), so a caller here IS a
 * role string — which is what makes four-eyes testable at all: the requester and the approver are
 * different roles in the ordinary case, and AC-miniloan-079's collision is reproduced by CONFIGURING
 * the approver role to be the requester's own (Operations), which is the only way one identity can
 * hold both capabilities under this scheme.
 *
 * <p><b>AC-miniloan-084 and AC-miniloan-085 are only half provable here.</b> What this unit records
 * — {@code approvedBy}, {@code approvedAt}, and the old value surviving on the adjustment row after
 * the account has moved on — is asserted below. The per-account HISTORY those two criteria are
 * written about has no endpoint and no screen anywhere upstream; GAP-miniloan-010 asks design where
 * it lives, and nothing is invented here in the meantime.
 */
@SpringBootTest
class ClosedAccountAdjustmentDecisionServiceTest {

    private static final String LOAN_OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ADJUSTMENT_APPROVER = "ROLE-005";

    @Autowired private ClosedAccountAdjustmentDecisionService decisions;
    @Autowired private ApproverRoleSettingService approverSettings;
    @Autowired private ClosedAccountAdjustmentRepository adjustments;
    @Autowired private ApproverRoleSettingRepository approverRoles;
    @Autowired private LoanAccountRepository accounts;
    @Autowired private PaymentRepository payments;

    @BeforeEach
    void clean() {
        adjustments.deleteAll();
        approverRoles.deleteAll();
    }

    /** The account AC-miniloan-076 starts from: closed, fully paid, assigned to Operations. */
    private LoanAccount closedAccount() {
        LoanAccount account =
                new LoanAccount(
                        UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000.00"), 12, OPERATIONS);
        account.reducePrincipal(account.getOutstandingPrincipal());
        account.close(LoanAccount.CloseReason.FullyPaid, Instant.parse("2026-08-31T04:00:00Z"));
        return accounts.save(account);
    }

    /** The last instalment of that account — the row AC-miniloan-076 says was recorded wrong. */
    private Payment lastPayment(LoanAccount account) {
        return payments.save(
                new Payment(
                        account.getId(),
                        UUID.randomUUID(),
                        new BigDecimal("8500.00"),
                        Payment.PaymentType.InstallmentExact,
                        null,
                        null,
                        OPERATIONS,
                        Instant.parse("2026-08-31T03:59:00Z")));
    }

    private ClosedAccountAdjustment filed(
            LoanAccount account,
            String targetRecordId,
            AdjustableField field,
            String oldValue,
            String newValue) {
        return adjustments.save(
                new ClosedAccountAdjustment(
                        account.getId(),
                        targetRecordId,
                        field,
                        oldValue,
                        newValue,
                        OPERATIONS,
                        Instant.parse("2026-09-01T02:00:00Z")));
    }

    @Nested
    class ApprovingARequest {

        /**
         * AC-miniloan-076's second step, end to end: ก. (Operations) filed it, ข. (the Supervisor the
         * setting names) approves it, and only now does the account take the new value.
         */
        @Test
        void theConfiguredApproverApprovesAndTheNewValueTakesEffect() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            DecisionResult result = decisions.approve(request.getId(), SUPERVISOR);

            assertThat(result.adjustment().getStatus()).isEqualTo(Status.Approved);
            assertThat(result.adjustment().getApprovedBy()).isEqualTo(SUPERVISOR);
            assertThat(result.adjustment().getApprovedAt())
                    .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
            assertThat(result.message())
                    .isEqualTo(
                            "อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ "
                                    + result.adjustment().getApprovedAt());
            assertThat(accounts.findById(account.getId()).orElseThrow().getCloseReason())
                    .isEqualTo(LoanAccount.CloseReason.EarlySettlement);
        }

        /**
         * AC-miniloan-084's other half: the adjustment is a record of its own, not an overwrite —
         * the old value is still readable from it after the account has moved on.
         */
        @Test
        void theOldValueIsStillReadableFromTheAdjustmentAfterwards() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            decisions.approve(request.getId(), SUPERVISOR);

            ClosedAccountAdjustment stored = adjustments.findById(request.getId()).orElseThrow();
            assertThat(stored.getOldValue()).isEqualTo("FullyPaid");
            assertThat(stored.getNewValue()).isEqualTo("EarlySettlement");
            assertThat(stored.getRequestedBy()).isEqualTo(OPERATIONS);
            assertThat(stored.getApprovedBy()).isEqualTo(SUPERVISOR);
        }

        /** ENT-010's LoanAccount.closedAt — an ISO-8601 instant, applied to the account. */
        @Test
        void theClosingTimeIsAdjusted() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSED_AT,
                            "2026-08-31T04:00:00Z",
                            "2026-08-30T09:30:00Z");

            decisions.approve(request.getId(), SUPERVISOR);

            assertThat(accounts.findById(account.getId()).orElseThrow().getClosedAt())
                    .isCloseTo(Instant.parse("2026-08-30T09:30:00Z"), within(1, ChronoUnit.MILLIS));
        }

        /** ENT-010's LoanAccount.assignedOperationsId — BR-miniloan-054@v1's owner of the account. */
        @Test
        void theAssignedOperationsPersonIsAdjusted() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_ASSIGNED_OPERATIONS_ID,
                            OPERATIONS,
                            "ROLE-004-another-person");

            decisions.approve(request.getId(), SUPERVISOR);

            assertThat(accounts.findById(account.getId()).orElseThrow().getAssignedOperationsId())
                    .isEqualTo("ROLE-004-another-person");
        }

        /**
         * ENT-010's Payment.amount, which is the very field AC-miniloan-076 is written about
         * ("ยอดชำระงวดสุดท้ายถูกบันทึกผิด"). The figure arrives in the {@code #,##0.00} form
         * {@code Money.exact} renders, separator and all, because that is the form a person copies
         * off the screen.
         */
        @Test
        void thePaymentAmountIsAdjustedFromTheFormTheScreenShows() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            Payment payment = lastPayment(account);
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            payment.getId().toString(),
                            AdjustableField.PAYMENT_AMOUNT,
                            "8,500.00",
                            "8,050.00");

            decisions.approve(request.getId(), SUPERVISOR);

            assertThat(payments.findById(payment.getId()).orElseThrow().getAmount())
                    .isEqualByComparingTo("8050.00");
        }

        /** ENT-010's Payment.recordedAt. */
        @Test
        void thePaymentTimeIsAdjusted() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            Payment payment = lastPayment(account);
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            payment.getId().toString(),
                            AdjustableField.PAYMENT_RECORDED_AT,
                            "2026-08-31T03:59:00Z",
                            "2026-08-29T08:00:00Z");

            decisions.approve(request.getId(), SUPERVISOR);

            assertThat(payments.findById(payment.getId()).orElseThrow().getRecordedAt())
                    .isCloseTo(Instant.parse("2026-08-29T08:00:00Z"), within(1, ChronoUnit.MILLIS));
        }

        /**
         * The approved value is applied and nothing downstream is recomputed — a deliberate stop,
         * not a gap in the code. BR-miniloan-021@v1 owns {@code outstandingPrincipal} and
         * {@code closeReason} and no rule in req's contract says a corrected payment moves them;
         * GAP-miniloan-009 is where that decision was asked for. This test exists so the day req
         * answers, it FAILS rather than passing quietly with the old behaviour.
         */
        @Test
        void approvingAPaymentCorrectionDoesNotRecomputeTheAccount() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            Payment payment = lastPayment(account);
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            payment.getId().toString(),
                            AdjustableField.PAYMENT_AMOUNT,
                            "8,500.00",
                            "8,050.00");

            decisions.approve(request.getId(), SUPERVISOR);

            LoanAccount stored = accounts.findById(account.getId()).orElseThrow();
            assertThat(stored.getOutstandingPrincipal()).isEqualByComparingTo("0.00");
            assertThat(stored.getCloseReason()).isEqualTo(LoanAccount.CloseReason.FullyPaid);
            assertThat(stored.getStatus()).isEqualTo(LoanAccount.Status.Closed);
        }

        /**
         * An unreadable new value refuses the whole decision — the request stays Pending and the
         * payment keeps the figure it had. A request marked Approved with nothing changed by it
         * would be the state BR-miniloan-041@v1 says must not exist.
         */
        @Test
        void anUnreadableNewValueRefusesTheDecisionAndChangesNothing() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            Payment payment = lastPayment(account);
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            payment.getId().toString(),
                            AdjustableField.PAYMENT_AMOUNT,
                            "8,500.00",
                            "แปดพันห้าสิบบาท");

            assertThatThrownBy(() -> decisions.approve(request.getId(), SUPERVISOR))
                    .isInstanceOf(AdjustedValueUnreadableException.class)
                    .hasMessageContaining("Payment.amount");

            assertThat(adjustments.findById(request.getId()).orElseThrow().getStatus())
                    .isEqualTo(Status.Pending);
            assertThat(payments.findById(payment.getId()).orElseThrow().getAmount())
                    .isEqualByComparingTo("8500.00");
        }

        /** STM-miniloan-004 leaves Pending once — ACL-016's condition, at this layer. */
        @Test
        void aSecondDecisionOnTheSameRequestIsRefused() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");
            decisions.approve(request.getId(), SUPERVISOR);

            assertThatThrownBy(() -> decisions.approve(request.getId(), SUPERVISOR))
                    .isInstanceOf(AdjustmentAlreadyDecidedException.class)
                    .hasMessage("คำขอปรับปรุงนี้ถูกพิจารณาไปแล้ว — พิจารณาซ้ำไม่ได้");
        }

        @Test
        void aRequestThatDoesNotExistIsNotFound() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);

            assertThatThrownBy(() -> decisions.approve(UUID.randomUUID(), SUPERVISOR))
                    .isInstanceOf(AdjustmentNotFoundException.class);
        }
    }

    @Nested
    class WhoMayDecide {

        /**
         * UC-miniloan-018's actor is "ผู้ถือ role ผู้อนุมัติตาม BR-miniloan-039@v1", so the setting is
         * the permission: with Supervisor configured, a Loan Officer is refused even though the Loan
         * Officer is the role that SET it.
         */
        @Test
        void aRoleThatIsNotTheConfiguredApproverIsRefused() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            assertThatThrownBy(() -> decisions.approve(request.getId(), LOAN_OFFICER))
                    .isInstanceOf(NotTheApproverException.class);

            assertThat(adjustments.findById(request.getId()).orElseThrow().getStatus())
                    .isEqualTo(Status.Pending);
            assertThat(accounts.findById(account.getId()).orElseThrow().getCloseReason())
                    .isEqualTo(LoanAccount.CloseReason.FullyPaid);
        }

        /**
         * ROLE-005 is the CAPABILITY ACL-016 names, not a caller of its own. A token presenting it
         * while the setting says Supervisor is refused — otherwise BR-miniloan-039@v1's setting would
         * decide nothing and AC-miniloan-079's collision could never occur.
         */
        @Test
        void theCapabilityRoleIsNotItselfAnApprover() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            assertThatThrownBy(() -> decisions.approve(request.getId(), ADJUSTMENT_APPROVER))
                    .isInstanceOf(NotTheApproverException.class);
        }

        /** BR-miniloan-040@v1 — with no approver named, nobody may decide either. */
        @Test
        void nobodyMayDecideWhileNoApproverRoleIsSet() {
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            assertThatThrownBy(() -> decisions.approve(request.getId(), SUPERVISOR))
                    .isInstanceOf(ApproverRoleNotSetException.class);
        }
    }

    @Nested
    class FourEyes {

        /**
         * AC-miniloan-079 word for word. The approver role is configured to Operations, so the very
         * person who filed the request now holds every permission there is — and is still refused,
         * with the request left at "รออนุมัติ" and the account untouched. "ถือ role ผู้อนุมัติยังไม่พอ
         * ต้องเป็นคนละคนด้วย".
         */
        @Test
        void theRequesterMayNotApproveTheirOwnRequestEvenHoldingTheApproverRole() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Operations);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            assertThatThrownBy(() -> decisions.approve(request.getId(), OPERATIONS))
                    .isInstanceOf(SelfApprovalRefusedException.class)
                    .hasMessage("อนุมัติคำขอของตัวเองไม่ได้ — ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้");

            assertThat(adjustments.findById(request.getId()).orElseThrow().getStatus())
                    .isEqualTo(Status.Pending);
            assertThat(accounts.findById(account.getId()).orElseThrow().getCloseReason())
                    .isEqualTo(LoanAccount.CloseReason.FullyPaid);
        }

        /** The same rule on the other decision — BR-miniloan-049@v1 is about who decides, not how. */
        @Test
        void theRequesterMayNotRejectTheirOwnRequestEither() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Operations);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            assertThatThrownBy(() -> decisions.reject(request.getId(), OPERATIONS))
                    .isInstanceOf(SelfApprovalRefusedException.class);
        }
    }

    @Nested
    class RejectingARequest {

        /** UC-miniloan-018's alternate flow — "ค่าเดิมของบัญชียังคงอยู่". */
        @Test
        void rejectingLeavesTheAccountWithTheValueItHad() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            DecisionResult result = decisions.reject(request.getId(), SUPERVISOR);

            assertThat(result.adjustment().getStatus()).isEqualTo(Status.Rejected);
            assertThat(result.adjustment().getApprovedBy()).isEqualTo(SUPERVISOR);
            assertThat(result.message()).isEqualTo("ปฏิเสธคำขอปรับปรุงแล้ว — ค่าเดิมของบัญชียังคงอยู่");
            assertThat(accounts.findById(account.getId()).orElseThrow().getCloseReason())
                    .isEqualTo(LoanAccount.CloseReason.FullyPaid);
        }
    }

    @Nested
    class WhatTheApproverReads {

        /** API-024 — the queue UI-miniloan-013 lists, oldest first, and only what is still waiting. */
        @Test
        void thePendingQueueHoldsOnlyUndecidedRequestsOldestFirst() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment first =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");
            ClosedAccountAdjustment second =
                    adjustments.save(
                            new ClosedAccountAdjustment(
                                    account.getId(),
                                    account.getId().toString(),
                                    AdjustableField.LOAN_ACCOUNT_ASSIGNED_OPERATIONS_ID,
                                    OPERATIONS,
                                    "ROLE-004-another-person",
                                    OPERATIONS,
                                    Instant.parse("2026-09-02T02:00:00Z")));

            List<ClosedAccountAdjustment> queue = decisions.listPending(SUPERVISOR);
            assertThat(queue).extracting(ClosedAccountAdjustment::getId)
                    .containsExactly(first.getId(), second.getId());

            decisions.approve(first.getId(), SUPERVISOR);

            // ACL-016's condition is state Pending, so a decided request LEAVES the queue and this
            // route has no way to ask for it back. Where the decided ones are read is GAP-miniloan-010.
            assertThat(decisions.listPending(SUPERVISOR))
                    .extracting(ClosedAccountAdjustment::getId)
                    .containsExactly(second.getId());
        }

        /**
         * API-025 — one request, and after a decision it carries the four-eyes record
         * AC-miniloan-084 asks for. Which SCREEN shows it is GAP-miniloan-010's question; that the
         * API can answer it is this unit's.
         */
        @Test
        void oneRequestCarriesTheApprovalRecordOnceItIsDecided() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
            LoanAccount account = closedAccount();
            ClosedAccountAdjustment request =
                    filed(
                            account,
                            account.getId().toString(),
                            AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                            "FullyPaid",
                            "EarlySettlement");

            ClosedAccountAdjustment beforeDecision = decisions.view(SUPERVISOR, request.getId());
            assertThat(beforeDecision.getApprovedBy()).isNull();
            assertThat(beforeDecision.getApprovedAt()).isNull();

            decisions.approve(request.getId(), SUPERVISOR);

            ClosedAccountAdjustment afterDecision = decisions.view(SUPERVISOR, request.getId());
            assertThat(afterDecision.getApprovedBy()).isEqualTo(SUPERVISOR);
            assertThat(afterDecision.getApprovedAt()).isNotNull();
        }

        /** rbac.json is default-deny, and ACL-016 is the only row for UC-miniloan-018. */
        @Test
        void aRoleThatIsNotTheApproverReadsNeither() {
            approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);

            assertThatThrownBy(() -> decisions.listPending(OPERATIONS))
                    .isInstanceOf(NotTheApproverException.class);
            assertThatThrownBy(() -> decisions.view(OPERATIONS, UUID.randomUUID()))
                    .isInstanceOf(NotTheApproverException.class);
        }
    }
}

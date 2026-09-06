package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.miniloan.domain.ApproverRoleSetting;
import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.repository.ApproverRoleSettingRepository;
import com.miniloan.service.ApproverRoleSettingService.ApproverRoleRequiredException;
import com.miniloan.service.ApproverRoleSettingService.LoanOfficerOnlyException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * UC-miniloan-019 · BR-miniloan-039@v1 · BR-miniloan-040@v1 · BR-miniloan-048@v1.
 *
 * <p>The mock-token scheme resolves one identity per role (FE-miniloan-002), so a caller here IS a
 * role string — the same reading {@code EarlyClosureSettlementService} and {@code Payment} already
 * work under, and the reason {@code updatedBy} is asserted as {@code ROLE-002} rather than as a
 * staff id.
 *
 * <p>AC-miniloan-089's second half — a request filed afterwards showing the NEW role, and the holder
 * of the old one being refused the approval — is measured where those things exist:
 * FE-miniloan-015 files the request and FE-miniloan-016 approves it. What is provable in this unit is
 * the half AC-miniloan-089 puts first: the value changes, it changes from configuration alone, and
 * every later read sees the new one with nothing restarted.
 */
@SpringBootTest
class ApproverRoleSettingServiceTest {

    private static final String APPLICANT = "ROLE-001";
    private static final String LOAN_OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";
    private static final String OPERATIONS = "ROLE-004";
    private static final String ADJUSTMENT_APPROVER = "ROLE-005";

    @Autowired private ApproverRoleSettingService service;
    @Autowired private ApproverRoleSettingRepository settings;

    @BeforeEach
    void clean() {
        settings.deleteAll();
    }

    /** BR-miniloan-040@v1: nothing is seeded, so the system starts with no approver at all. */
    @Test
    void startsWithNoApproverRoleSet() {
        assertThat(service.currentApproverRole()).isEmpty();
        assertThat(service.view(LOAN_OFFICER)).isEmpty();
        assertThat(settings.count()).isZero();
    }

    @Nested
    class SettingItForTheFirstTime {

        /** AC-miniloan-082 — the sentence, the stored value, and who set it. */
        @Test
        void loanOfficerSetsItAndTheSystemSaysSo() {
            Instant before = Instant.now().minusSeconds(1);

            var outcome = service.set(LOAN_OFFICER, ApproverRole.Supervisor);

            assertThat(outcome.firstTime()).isTrue();
            assertThat(outcome.message())
                    .isEqualTo("ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น Supervisor เรียบร้อย");
            assertThat(outcome.setting().getApproverRole()).isEqualTo(ApproverRole.Supervisor);
            assertThat(outcome.setting().getUpdatedBy()).isEqualTo(LOAN_OFFICER);
            assertThat(outcome.setting().getUpdatedAt())
                    .isAfterOrEqualTo(before)
                    .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        }

        /**
         * AC-miniloan-082's last clause: the door BR-miniloan-040@v1 held shut is now open. This is
         * the exact reading FE-miniloan-015 does before it will accept an adjustment request.
         */
        @Test
        void theGateBrMiniloan040HeldShutIsNowOpen() {
            assertThat(service.currentApproverRole()).isEmpty();

            service.set(LOAN_OFFICER, ApproverRole.Operations);

            assertThat(service.currentApproverRole()).contains(ApproverRole.Operations);
        }

        /** ENT-011 keeps one row, and the constraint on it is the database's, not the service's. */
        @Test
        void writesTheOneRowUnderTheOneKey() {
            service.set(LOAN_OFFICER, ApproverRole.LoanOfficer);

            assertThat(settings.count()).isEqualTo(1);
            assertThat(settings.findBySingletonKey(ApproverRoleSetting.SINGLETON_KEY))
                    .get()
                    .extracting(ApproverRoleSetting::getApproverRole)
                    .isEqualTo(ApproverRole.LoanOfficer);
        }
    }

    @Nested
    class WhoMaySetIt {

        /** AC-miniloan-083, word for word — and Operations is the role the criterion names first. */
        @Test
        void operationsIsRefusedAndNothingIsWritten() {
            assertThatThrownBy(() -> service.set(OPERATIONS, ApproverRole.Operations))
                    .isInstanceOf(LoanOfficerOnlyException.class)
                    .hasMessage("ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer");

            assertThat(service.currentApproverRole()).isEmpty();
            assertThat(settings.count()).isZero();
        }

        /** AC-miniloan-083 says the Applicant gives the same result, so it is asserted the same way. */
        @Test
        void applicantIsRefusedTheSameWay() {
            assertThatThrownBy(() -> service.set(APPLICANT, ApproverRole.Supervisor))
                    .isInstanceOf(LoanOfficerOnlyException.class)
                    .hasMessage("ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer");

            assertThat(settings.count()).isZero();
        }

        /**
         * {@code rbac.json} is default-deny and ACL-017 names ROLE-002 alone, so the two roles no
         * criterion bothers to mention are refused for the same reason rather than by omission —
         * including ROLE-005, whose whole job is approving the adjustments this setting governs.
         */
        @Test
        void everyOtherRoleIsRefusedBecauseTheMatrixIsDefaultDeny() {
            assertThatThrownBy(() -> service.set(SUPERVISOR, ApproverRole.Supervisor))
                    .isInstanceOf(LoanOfficerOnlyException.class);
            assertThatThrownBy(() -> service.set(ADJUSTMENT_APPROVER, ApproverRole.Supervisor))
                    .isInstanceOf(LoanOfficerOnlyException.class);
            assertThatThrownBy(() -> service.set(null, ApproverRole.Supervisor))
                    .isInstanceOf(LoanOfficerOnlyException.class);

            assertThat(settings.count()).isZero();
        }

        /**
         * AC-miniloan-083's "ค่า role ผู้อนุมัติไม่เปลี่ยน" — the half that only shows up once there
         * is something to change. A refusal that reached the row would be invisible in the empty case.
         */
        @Test
        void aRefusedCallerLeavesAnExistingValueExactlyAsItWas() {
            service.set(LOAN_OFFICER, ApproverRole.Supervisor);

            assertThatThrownBy(() -> service.set(OPERATIONS, ApproverRole.Operations))
                    .isInstanceOf(LoanOfficerOnlyException.class);

            assertThat(service.currentApproverRole()).contains(ApproverRole.Supervisor);
            assertThat(settings.count()).isEqualTo(1);
        }

        /** Reading answers to ACL-017 too — see the service comment on why API-022 has no row of its own. */
        @Test
        void readingIsGatedByTheSameEntry() {
            service.set(LOAN_OFFICER, ApproverRole.Supervisor);

            assertThat(service.view(LOAN_OFFICER)).isPresent();
            assertThatThrownBy(() -> service.view(OPERATIONS))
                    .isInstanceOf(LoanOfficerOnlyException.class)
                    .hasMessage("ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer");
        }
    }

    @Nested
    class ChangingIt {

        /** AC-miniloan-089 — a different sentence from AC-miniloan-082, because it is a different event. */
        @Test
        void changingItSaysChangedRatherThanSet() {
            service.set(LOAN_OFFICER, ApproverRole.Supervisor);

            var outcome = service.set(LOAN_OFFICER, ApproverRole.Operations);

            assertThat(outcome.firstTime()).isFalse();
            assertThat(outcome.message()).isEqualTo("เปลี่ยน role ผู้อนุมัติเป็น Operations เรียบร้อย");
            assertThat(outcome.setting().getApproverRole()).isEqualTo(ApproverRole.Operations);
        }

        /**
         * AC-miniloan-089's "ไม่ต้องแก้โปรแกรมและไม่ต้องนำระบบขึ้นใหม่": every read goes to the row,
         * so the value in force after a change is the new one immediately — no restart, and no cached
         * field that would have to be invalidated to make the criterion true.
         */
        @Test
        void everyReadAfterAChangeSeesTheNewValue() {
            service.set(LOAN_OFFICER, ApproverRole.Supervisor);
            assertThat(service.currentApproverRole()).contains(ApproverRole.Supervisor);

            service.set(LOAN_OFFICER, ApproverRole.LoanOfficer);

            assertThat(service.currentApproverRole()).contains(ApproverRole.LoanOfficer);
            assertThat(service.view(LOAN_OFFICER))
                    .get()
                    .extracting(ApproverRoleSetting::getApproverRole)
                    .isEqualTo(ApproverRole.LoanOfficer);
        }

        /** Change it three times and there is still one row — the setting has one answer by construction. */
        @Test
        void changingItRepeatedlyNeverGrowsASecondRow() {
            service.set(LOAN_OFFICER, ApproverRole.Supervisor);
            service.set(LOAN_OFFICER, ApproverRole.Operations);
            service.set(LOAN_OFFICER, ApproverRole.LoanOfficer);

            assertThat(settings.count()).isEqualTo(1);
            assertThat(service.currentApproverRole()).contains(ApproverRole.LoanOfficer);
        }

        /** Setting the same role again is still a change, and it moves who set it and when. */
        @Test
        void settingTheSameRoleAgainIsRecordedAsAChange() {
            var first = service.set(LOAN_OFFICER, ApproverRole.Supervisor);

            var again = service.set(LOAN_OFFICER, ApproverRole.Supervisor);

            assertThat(again.firstTime()).isFalse();
            assertThat(again.setting().getId()).isEqualTo(first.setting().getId());
            assertThat(again.setting().getApproverRole()).isEqualTo(ApproverRole.Supervisor);
        }

        /** BR-miniloan-040@v1 has no rule for un-setting, so the service refuses rather than inventing one. */
        @Test
        void clearingItIsRefused() {
            service.set(LOAN_OFFICER, ApproverRole.Supervisor);

            assertThatThrownBy(() -> service.set(LOAN_OFFICER, null))
                    .isInstanceOf(ApproverRoleRequiredException.class)
                    .hasMessage("ต้องระบุ role ผู้อนุมัติ — ล้างค่าที่ตั้งไว้ให้ว่างไม่ได้");

            assertThat(service.currentApproverRole()).contains(ApproverRole.Supervisor);
        }
    }
}

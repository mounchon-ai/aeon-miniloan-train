package com.miniloan.service;

import com.miniloan.domain.ApproverRoleSetting;
import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.repository.ApproverRoleSettingRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้ว (UC-miniloan-019 · API-019 · API-022 · ACL-017).
 *
 * <p><b>Loan Officer, and nobody else</b> (BR-miniloan-048@v1 · AC-miniloan-083). ACL-017 is the
 * only entry {@code rbac.json} carries for UC-miniloan-019 and its {@code defaultEffect} is deny, so
 * every other role is refused here — including Operations, which is the role that most often files
 * the adjustment requests this setting decides the approver of. That is the point of the rule: the
 * side that asks for a change does not get to choose who approves it.
 *
 * <p><b>Reading is gated the same way, and that is a decision worth seeing.</b> API-022 has no ACL
 * row of its own; ACL-017 covers the use case, and UC-miniloan-019's actor is the Loan Officer. With
 * a default-deny matrix, a second role on this route would be a permission this unit invented rather
 * than one design granted — the same line {@code RepaymentScheduleController} drew on API-012. If
 * Operations should be able to READ the setting over HTTP, that is an entry design has to add.
 *
 * <p><b>The units that ENFORCE this setting do not go through the HTTP gate at all.</b>
 * FE-miniloan-015 refuses an adjustment request when no approver role is set (BR-miniloan-040@v1)
 * and FE-miniloan-016 checks who may approve one (BR-miniloan-049@v1); both ask
 * {@link #currentApproverRole()}, which carries no role check because it is not a caller — it is the
 * rule reading its own configuration. Putting the ACL only on the two routes keeps the permission
 * about WHO MAY SET IT, which is what BR-miniloan-048@v1 actually says.
 *
 * <p><b>Nothing is cached.</b> AC-miniloan-089 ends with "ไม่ต้องแก้โปรแกรมและไม่ต้องนำระบบขึ้นใหม่",
 * so the row is read on every call. A field holding the value would pass the change test in one
 * process and fail the criterion in the deployment it describes.
 */
@Service
public class ApproverRoleSettingService {

    /** ACL-017 names ROLE-002 — เจ้าหน้าที่สินเชื่อ, the Loan Officer of BR-miniloan-048@v1. */
    private static final String LOAN_OFFICER = "ROLE-002";

    private final ApproverRoleSettingRepository settings;
    private final Clock clock;

    public ApproverRoleSettingService(ApproverRoleSettingRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /** AC-miniloan-083's sentence, word for word — and the same one for reading and for writing. */
    public static class LoanOfficerOnlyException extends RuntimeException {
        public LoanOfficerOnlyException() {
            super("ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer");
        }
    }

    /**
     * ENT-011 lets the stored value be null, but SETTING it to nothing is not a thing any criterion
     * asks for, and BR-miniloan-040@v1 treats "no approver" as a state to leave rather than one to
     * enter. Clearing the setting would need a rule that says what happens to the requests already
     * waiting on it, and no such rule exists.
     */
    public static class ApproverRoleRequiredException extends RuntimeException {
        public ApproverRoleRequiredException() {
            super("ต้องระบุ role ผู้อนุมัติ — ล้างค่าที่ตั้งไว้ให้ว่างไม่ได้");
        }
    }

    /**
     * What one accepted setting did. {@code firstTime} is the difference between AC-miniloan-082's
     * sentence and AC-miniloan-089's, and it is a fact about the row, not about the caller.
     */
    public record SettingOutcome(ApproverRoleSetting setting, boolean firstTime) {

        /** AC-miniloan-082 and AC-miniloan-089 word for word, chosen by which one happened. */
        public String message() {
            return firstTime
                    ? "ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น "
                            + setting.getApproverRole()
                            + " เรียบร้อย"
                    : "เปลี่ยน role ผู้อนุมัติเป็น " + setting.getApproverRole() + " เรียบร้อย";
        }
    }

    /**
     * API-019. AC-miniloan-082 sets it for the first time; AC-miniloan-089 changes it afterwards, and
     * both land on the same row — the second call updates rather than inserting, which is why a
     * second settings row never exists to disagree with the first.
     *
     * <p>The role name travels through as ENT-011 spells it ({@code LoanOfficer} · {@code Supervisor}
     * · {@code Operations}). Design gives these three no Thai label anywhere — {@code rbac.json}'s
     * Thai labels belong to ROLE-001..005, which is a different list — so no display name is invented
     * here; rendering one is the web app's business once design names them.
     */
    @Transactional
    public SettingOutcome set(String callerRole, ApproverRole approverRole) {
        // Who may set it does not depend on what it is being set to, so this is asked first.
        if (!LOAN_OFFICER.equals(callerRole)) {
            throw new LoanOfficerOnlyException();
        }
        if (approverRole == null) {
            throw new ApproverRoleRequiredException();
        }

        Instant now = Instant.now(clock);
        Optional<ApproverRoleSetting> existing =
                settings.findBySingletonKey(ApproverRoleSetting.SINGLETON_KEY);

        if (existing.isEmpty()) {
            ApproverRoleSetting created =
                    settings.save(new ApproverRoleSetting(approverRole, callerRole, now));
            return new SettingOutcome(created, true);
        }

        // AC-miniloan-089: "ตั้งไว้เป็น role หนึ่งอยู่แล้ว" — so this reads as a change even when the
        // row was somehow left with a null value, because a row that exists has been touched before.
        ApproverRoleSetting current = existing.get();
        current.changeTo(approverRole, callerRole, now);
        return new SettingOutcome(settings.save(current), false);
    }

    /** API-022 — the value in force, for the Loan Officer who may change it. */
    @Transactional(readOnly = true)
    public Optional<ApproverRoleSetting> view(String callerRole) {
        if (!LOAN_OFFICER.equals(callerRole)) {
            throw new LoanOfficerOnlyException();
        }
        return settings.findBySingletonKey(ApproverRoleSetting.SINGLETON_KEY);
    }

    /**
     * BR-miniloan-040@v1's question, for the rules that enforce it rather than for a caller: empty
     * means no approver role has been set, and FE-miniloan-015 refuses every adjustment request while
     * that is true. Deliberately ungated — see the class comment.
     */
    @Transactional(readOnly = true)
    public Optional<ApproverRole> currentApproverRole() {
        return settings
                .findBySingletonKey(ApproverRoleSetting.SINGLETON_KEY)
                .flatMap(ApproverRoleSetting::approverRoleValue);
    }
}

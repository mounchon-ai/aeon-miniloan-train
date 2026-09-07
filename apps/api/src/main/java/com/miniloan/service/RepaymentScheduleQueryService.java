package com.miniloan.service;

import com.miniloan.domain.Installment;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanApplication;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ดูตารางผ่อนชำระ (UC-miniloan-011 · API-012 · BR-miniloan-018@v1 · BR-miniloan-033@v1).
 *
 * <p><b>The whole table, not the part still ahead</b> (AC-miniloan-098). Every instalment of the
 * revision in force is returned, from number 1 to the last, each carrying the status STM-miniloan-003
 * gave it. Nothing is filtered by due date and nothing is filtered by status: an instalment already
 * paid is exactly as much a row of the borrower's schedule as one still due, and dropping it would
 * be the one thing BR-miniloan-018@v1 says out loud not to do.
 *
 * <p><b>The ownership refusal lives here, not on the screen</b> (BR-miniloan-033@v1 ·
 * AC-miniloan-099). ENT-006 holds no applicant of its own — an account is owned through the
 * application it was disbursed from — so the owner is read off {@link LoanApplication#getApplicantId()}
 * and compared with the caller. Because the check is in the service, the answer is the same whether
 * the caller came from UI-miniloan-004 or typed the URL, which is what "ไม่ใช่แค่ไม่แสดงลิงก์บนหน้าจอ"
 * asks for. ACL-010 names {@code enforceAt: [api, domain]} and this is the domain half; the route is
 * the other.
 *
 * <p><b>Order of the three guards.</b> Not found, then owner, then Active — the same order the rest
 * of this codebase reads its account guards in. The owner check comes before the state check on
 * purpose: a stranger must not be able to tell an Active account from a Closed one by the refusal
 * they get back.
 *
 * <p><b>What is deliberately absent.</b> API-012 declares one operation, "ดูตารางผ่อน<b>ปัจจุบัน</b>",
 * and no parameter — so a superseded revision has no declared surface here and none is invented, even
 * though {@link RepaymentScheduleReissueService}'s note hands those rows to this unit. The rows are
 * kept and readable (AC-miniloan-010 proves that); what is missing is a designed way to ask for them,
 * and that is a question for design, not a query parameter added on the way past.
 */
@Service
public class RepaymentScheduleQueryService {

    private final LoanAccountRepository accounts;
    private final LoanApplicationRepository applications;
    private final RepaymentScheduleRepository schedules;
    private final InstallmentRepository installments;
    private final LoanAccountScopeService scope;

    public RepaymentScheduleQueryService(
            LoanAccountRepository accounts,
            LoanApplicationRepository applications,
            RepaymentScheduleRepository schedules,
            InstallmentRepository installments,
            LoanAccountScopeService scope) {
        this.accounts = accounts;
        this.applications = applications;
        this.schedules = schedules;
        this.installments = installments;
        this.scope = scope;
    }

    public static class LoanAccountNotFoundException extends RuntimeException {
        public LoanAccountNotFoundException(UUID id) {
            super("ไม่พบบัญชีสินเชื่อ " + id);
        }
    }

    /** AC-miniloan-099, word for word — BR-miniloan-033@v1 refused at the API. */
    public static class NotAccountOwnerException extends RuntimeException {
        public NotAccountOwnerException() {
            super("ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้");
        }
    }

    /** ACL-010's condition and UC-miniloan-011's precondition: the account must be Active. */
    public static class AccountNotActiveException extends RuntimeException {
        public AccountNotActiveException() {
            super("ดูตารางผ่อนไม่ได้ — บัญชีนี้ปิดแล้ว");
        }
    }

    /**
     * An Active account always holds a current revision, because disbursement issues revision 1 in
     * the same transaction that opens the account (BR-miniloan-015@v1). Refusing by name beats
     * handing back an empty table that reads like a borrower with nothing left to pay.
     */
    public static class NoCurrentScheduleException extends RuntimeException {
        public NoCurrentScheduleException(UUID loanAccountId) {
            super("บัญชี " + loanAccountId + " ไม่มีตารางผ่อนฉบับที่ใช้อยู่");
        }
    }

    /** The revision in force and every one of its rows, instalment order. */
    public record ScheduleView(
            LoanAccount account, RepaymentSchedule schedule, List<Installment> installments) {}

    @Transactional(readOnly = true)
    public ScheduleView view(UUID loanAccountId, String applicantId) {
        LoanAccount account =
                accounts
                        .findById(loanAccountId)
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));

        LoanApplication application =
                applications
                        .findById(account.getApplicationId())
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));
        if (!application.getApplicantId().equals(applicantId)) {
            throw new NotAccountOwnerException();
        }

        if (account.getStatus() != LoanAccount.Status.Active) {
            throw new AccountNotActiveException();
        }

        RepaymentSchedule current =
                schedules
                        .findByLoanAccountIdAndCurrentIsTrue(loanAccountId)
                        .orElseThrow(() -> new NoCurrentScheduleException(loanAccountId));

        return new ScheduleView(
                account,
                current,
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId()));
    }

    /**
     * ACL-020 · ACL-033 — the Operations person reads the schedule of an account assigned to them
     * (FE-miniloan-027).
     *
     * <p><b>Why a second entry point rather than a role added to {@link #view}.</b> The two are
     * different scopes answering to different entries. {@code view} is ACL-010: the Applicant who
     * owns the account, and it keeps its {@code Active} precondition because ACL-010 declares one.
     * This one is ACL-020's "ดูและดำเนินการกับบัญชีสินเชื่อที่ตนถูก assign", which declares NO
     * condition — and UI-miniloan-012 is where a Closed account's adjustment is filed (ACL-015), so
     * an Operations person who could not open a Closed account's table could not use the screen the
     * criterion is about. Adding the Active check here would be inventing a condition design did not
     * write; adding ROLE-004 to {@code view} would delete one design did.
     *
     * <p>The refusal is {@link LoanAccountScopeService.NotVisibleException} — the one sentence that
     * covers "not yours", "no scope" and "not there" alike, so an Operations caller cannot tell an
     * account that exists from one that does not, which is the whole of BR-miniloan-054@v1.
     */
    @Transactional(readOnly = true)
    public ScheduleView viewAsAssignedOperations(UUID loanAccountId, String operationsId) {
        LoanAccount account = scope.visibleTo(operationsId, loanAccountId);

        RepaymentSchedule current =
                schedules
                        .findByLoanAccountIdAndCurrentIsTrue(loanAccountId)
                        .orElseThrow(() -> new NoCurrentScheduleException(loanAccountId));

        return new ScheduleView(
                account,
                current,
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId()));
    }
}

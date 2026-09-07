package com.miniloan.service;

import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * แดชบอร์ดภาพรวมสถานะ (UC-miniloan-020 · API-020 · BR-miniloan-024@v1 · ACL-018).
 *
 * <p><b>Five numbers out of two aggregates</b> (AC-miniloan-110). Submitted / UnderReview / Approved
 * are counted on {@link LoanApplication} and Active / Closed on {@link LoanAccount}; the two are
 * never joined and never added together. Each square is one {@code count} in the database rather
 * than a list loaded and sized, so an empty system costs the same as a full one and no row ever
 * reaches this layer.
 *
 * <p><b>Disbursed has no square, and that is the point of AC-miniloan-112.</b> Every bucket is an
 * exact status match, so an application that reached Disbursed leaves the Approved count the moment
 * it is disbursed and the account it produced enters the Active count — counted once, on the account
 * side. Nothing here adds an application bucket to an account bucket, which is the shape the
 * criterion calls "จุดที่การนับข้ามสอง Aggregate พลาดได้ง่ายที่สุด".
 *
 * <p><b>Zero is a number, not an absence</b> (AC-miniloan-111). {@link DashboardSummary} carries
 * primitive {@code long}s, so there is no value this service can return that renders as a blank, a
 * dash or "no data"; an empty system produces five honest zeros and no branch anywhere treats that
 * as an error.
 *
 * <p><b>The role guard is ACL-018's domain half</b> ({@code enforceAt: [api, domain]}). rbac.json is
 * default-deny and ACL-018 is the only entry naming UC-miniloan-020, so every role but ROLE-002 is
 * refused here rather than being handed five zeros — an unpermitted caller is not asking a question
 * with an empty answer, they are asking a question they may not ask. The refusal carries one
 * sentence for every reason, the same idiom {@link LoanAccountScopeService.NotVisibleException} uses.
 *
 * <p><b>Why the counts are system-wide although ACL-018 says {@code scope: own}</b> — a decision
 * that belongs upstream and is recorded here because the code had to take one. DQ-miniloan-004's
 * answer reads "นับเฉพาะใบสมัครและบัญชีที่ตัวเองถูกมอบหมาย", but that scope cannot be implemented
 * against the rest of the design:
 *
 * <ul>
 *   <li>{@link LoanApplication#assignTo} refuses any status but UnderReview (STM-miniloan-001 has no
 *       Submitted→assigned edge), so a Submitted application can never carry an
 *       {@code assignedLoanOfficerId} and the "ยื่นแล้ว" square would be structurally 0 forever.
 *   <li>ENT-006 has no loan-officer attribute and rbac.json grants ROLE-002 no scope over a loan
 *       account at all — {@link LoanAccountScopeService} gives one to ROLE-001 and ROLE-004 and to
 *       nobody else — so "ใช้งานอยู่" and "ปิดแล้ว" would be 0 as well.
 * </ul>
 *
 * <p>Strict {@code own} therefore yields 0 · 2 · 1 · 0 · 0 against AC-miniloan-110's 3 · 2 · 1 · 5 · 4,
 * and the only way to make that criterion pass would be to rewrite the criterion. AC-miniloan-112
 * itself parks the scope question ("ขอบเขตข้อมูลที่ Loan Officer แต่ละคนเห็นยังไม่ตัดสิน") and names
 * DQ-miniloan-004, whose {@code raised_by} is BR-miniloan-033@v1 — a rule about an <em>applicant</em>
 * owning their accounts. The counts are system-wide because that is what the acceptance criteria
 * measure; a card is open with design to settle whether ACL-018's {@code own} survives.
 */
@Service
public class DashboardSummaryService {

    /** เจ้าหน้าที่สินเชื่อ — the only role ACL-018 admits. */
    private static final String LOAN_OFFICER = "ROLE-002";

    private final LoanApplicationRepository applications;
    private final LoanAccountRepository accounts;

    public DashboardSummaryService(
            LoanApplicationRepository applications, LoanAccountRepository accounts) {
        this.applications = applications;
        this.accounts = accounts;
    }

    /** rbac.json's default effect, said out loud — ACL-018 names ROLE-002 and no other entry names this use case. */
    public static class DashboardNotPermittedException extends RuntimeException {
        public DashboardNotPermittedException() {
            super("บทบาทนี้ไม่มีสิทธิ์ดูแดชบอร์ดภาพรวมสถานะ");
        }
    }

    /**
     * The five squares of UI-miniloan-009, in the order AC-miniloan-110 reads them. The names say
     * which aggregate each number came from, because the criterion's whole warning is that they look
     * like one series and are not.
     */
    public record DashboardSummary(
            long submittedApplications,
            long underReviewApplications,
            long approvedApplications,
            long activeLoanAccounts,
            long closedLoanAccounts) {}

    @Transactional(readOnly = true)
    public DashboardSummary summaryFor(String callerRole) {
        if (!LOAN_OFFICER.equals(callerRole)) {
            throw new DashboardNotPermittedException();
        }
        return new DashboardSummary(
                applications.countByStatus(LoanApplication.Status.Submitted),
                applications.countByStatus(LoanApplication.Status.UnderReview),
                applications.countByStatus(LoanApplication.Status.Approved),
                accounts.countByStatus(LoanAccount.Status.Active),
                accounts.countByStatus(LoanAccount.Status.Closed));
    }
}

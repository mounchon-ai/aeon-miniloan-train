package com.miniloan.service;

import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may see which loan account (API-013 · API-023 · BR-miniloan-033@v1 · BR-miniloan-054@v1 ·
 * ACL-020).
 *
 * <p><b>The scope is a WHERE clause, not a filter over everything.</b> AC-miniloan-137's whole point
 * is that the hole lives in the list rather than in the single read, so neither method here ever
 * holds a collection that contains an account the caller may not see. {@link #visibleTo(String)}
 * queries by the caller's own key; {@link #visibleTo(String, UUID)} reads one row and then asks the
 * same question of it, so the two can never disagree about what is visible.
 *
 * <p>Two roles have a scope and the rest have none, because that is what design declares:
 * BR-miniloan-054@v1 gives Operations the accounts assigned to them, BR-miniloan-033@v1 gives an
 * Applicant the accounts behind their own applications, and {@code rbac.json} is default-deny with
 * no other entry naming a loan account. A supervisor seeing their team's accounts is explicitly
 * still undecided (BR-miniloan-054@v1 defers it to DQ-miniloan-002/003/004), so it is not answered
 * here.
 */
@Service
public class LoanAccountScopeService {

    private static final String APPLICANT = "ROLE-001";
    private static final String OPERATIONS = "ROLE-004";

    private final LoanAccountRepository accounts;
    private final LoanApplicationRepository applications;

    public LoanAccountScopeService(
            LoanAccountRepository accounts, LoanApplicationRepository applications) {
        this.accounts = accounts;
        this.applications = applications;
    }

    /**
     * One sentence for "not yours", for "no scope at all" and for "not there". A refusal that told
     * the three apart would be a way to enumerate accounts belonging to other people, which is the
     * thing BR-miniloan-054@v1 and BR-miniloan-033@v1 both exist to stop.
     */
    public static class NotVisibleException extends RuntimeException {
        public NotVisibleException() {
            super("ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้");
        }
    }

    @Transactional(readOnly = true)
    public List<LoanAccount> visibleTo(String callerRole) {
        if (OPERATIONS.equals(callerRole)) {
            return accounts.findByAssignedOperationsIdOrderByDisbursedAtAsc(callerRole);
        }
        if (APPLICANT.equals(callerRole)) {
            List<UUID> ownApplications =
                    applications.findByApplicantId(callerRole).stream().map(LoanApplication::getId).toList();
            return ownApplications.isEmpty()
                    ? List.of()
                    : accounts.findByApplicationIdInOrderByDisbursedAtAsc(ownApplications);
        }
        // Default-deny: a role with no declared scope gets an empty list, never everything.
        return List.of();
    }

    @Transactional(readOnly = true)
    public LoanAccount visibleTo(String callerRole, UUID loanAccountId) {
        LoanAccount account = accounts.findById(loanAccountId).orElseThrow(NotVisibleException::new);
        if (OPERATIONS.equals(callerRole)) {
            if (!account.getAssignedOperationsId().equals(callerRole)) {
                throw new NotVisibleException();
            }
            return account;
        }
        if (APPLICANT.equals(callerRole)) {
            LoanApplication application =
                    applications.findById(account.getApplicationId()).orElseThrow(NotVisibleException::new);
            if (!application.getApplicantId().equals(callerRole)) {
                throw new NotVisibleException();
            }
            return account;
        }
        throw new NotVisibleException();
    }
}

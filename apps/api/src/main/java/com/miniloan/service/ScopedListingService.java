package com.miniloan.service;

import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.LoanApplicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ดูรายการใบสมัครที่มีสิทธิ์เห็น (UC-miniloan-023 · API-005 · BR-miniloan-033@v1 · ACL-019).
 *
 * <p><b>The scope is a WHERE clause, not a filter over everything</b> — the same shape {@link
 * LoanAccountScopeService} uses on the account side, and for the reason AC-miniloan-128 states
 * outright: "ช่องโหว่ของขอบเขตข้อมูลมักอยู่ที่การเรียกแบบรายการ ไม่ใช่รายใบ เพราะรายใบมักมีการตรวจ
 * แต่รายการมักลืม". There is no moment at which every application has been loaded and only the
 * rendering is scoped, so the criterion's count half — "จำนวนรายการที่ได้ตรงกับจำนวนใบของ ก.
 * เท่านั้น" — is answered by the query rather than by what a caller happens to render.
 *
 * <p><b>Two roles have a scope over an application list and the rest have none</b>, because that is
 * what rbac.json declares and its default effect is deny:
 *
 * <ul>
 *   <li>ROLE-001 — ACL-019 {@code scope: own}: the applications that person filed, matched on
 *       {@code applicantId}.
 *   <li>ROLE-003 — ACL-003 and ACL-031 both {@code scope: all}: the supervisor assigns work and
 *       reads the unassigned queue, so every application is in scope. {@link
 *       LoanApplicationSubmitService#findDetail} already gives that role the same answer on the
 *       single read, and a list that disagreed with the detail would be two scopes for one role.
 *   <li>ROLE-002 — ACL-027 {@code scope: own} means "assigned to me" and is deliberately NOT served
 *       here. {@code findDetail} refuses the same role for the same reason and names the unit that
 *       owns it; UI-miniloan-006's queue has no unit that built its API surface yet, and inventing
 *       the branch under UC-miniloan-023 — whose actor is the Applicant and whose three criteria
 *       never mention an officer — would be this unit answering a question design asked elsewhere.
 * </ul>
 *
 * <p><b>One sentence for "not yours", for "no scope" and for "not there"</b> ({@link
 * ApplicationNotVisibleException}). AC-miniloan-127 quotes it — "ไม่มีสิทธิ์เข้าถึงใบสมัครนี้" — and
 * it is the applications-side twin of the refusal {@code LoanAccountScopeService} already gives for
 * the same rule. A refusal that told the three apart would be a way to learn which application ids
 * exist, which is the thing BR-miniloan-033@v1 exists to stop.
 */
@Service
public class ScopedListingService {

    private static final String APPLICANT = "ROLE-001";
    private static final String SUPERVISOR = "ROLE-003";

    private final LoanApplicationRepository applications;

    public ScopedListingService(LoanApplicationRepository applications) {
        this.applications = applications;
    }

    /**
     * AC-miniloan-127, word for word. Carries no id: naming the application a caller may not see
     * would put the very identifier the refusal exists to protect into the response.
     */
    public static class ApplicationNotVisibleException extends RuntimeException {
        public ApplicationNotVisibleException() {
            super("ไม่มีสิทธิ์เข้าถึงใบสมัครนี้");
        }
    }

    /** A role rbac.json gives no application list at all — default-deny said out loud. */
    public static class ListingNotPermittedException extends RuntimeException {
        public ListingNotPermittedException() {
            super("บทบาทนี้ไม่มีสิทธิ์ดูรายการใบสมัคร");
        }
    }

    /**
     * API-005 — "ดูรายการใบสมัครที่ผู้เรียกมีสิทธิ์เห็น". Oldest first, so a caller with several
     * applications reads them in the order they were filed rather than in whatever order the
     * database happened to return.
     */
    @Transactional(readOnly = true)
    public List<LoanApplication> applicationsVisibleTo(String callerRole) {
        if (APPLICANT.equals(callerRole)) {
            return applications.findByApplicantIdOrderByCreatedAtAsc(callerRole);
        }
        if (SUPERVISOR.equals(callerRole)) {
            return applications.findAllByOrderByCreatedAtAsc();
        }
        throw new ListingNotPermittedException();
    }
}

package com.miniloan.service;

import com.miniloan.domain.CreditAssessment;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.LoanApplicationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
 *   <li>ROLE-002 — ACL-027 {@code scope: own}, where "own" means <b>assigned to me</b>: the
 *       applications whose {@code assignedLoanOfficerId} is this officer, and never "every
 *       application". FE-miniloan-019 left this branch out on purpose and said so — UC-miniloan-023's
 *       actor is the Applicant and its three criteria never mention an officer, so the branch had no
 *       screen behind it then. FE-miniloan-023 builds UI-miniloan-006, the officer's queue, which is
 *       the screen ACL-027 was written for ({@code enforceAt: [api, domain]}), so it lands here now.
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
    private static final String OFFICER = "ROLE-002";
    private static final String SUPERVISOR = "ROLE-003";

    private final LoanApplicationRepository applications;
    private final CreditAssessmentRepository assessments;

    public ScopedListingService(
            LoanApplicationRepository applications, CreditAssessmentRepository assessments) {
        this.applications = applications;
        this.assessments = assessments;
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
        if (OFFICER.equals(callerRole)) {
            return applications.findByAssignedLoanOfficerIdOrderByCreatedAtAsc(callerRole);
        }
        if (SUPERVISOR.equals(callerRole)) {
            return applications.findAllByOrderByCreatedAtAsc();
        }
        throw new ListingNotPermittedException();
    }

    /**
     * ENT-003's band for each row of a list, keyed by application id.
     *
     * <p>UI-miniloan-006 puts "Credit Band" on every row of the officer's queue and screens.json
     * binds that field to ENT-003, but API-005 returns applications and the band lives on the
     * assessment — the same shape of hole {@code submittedAt} was in when FE-miniloan-021 found it.
     * The alternative on the web side would be one detail call per row, which would make a queue's
     * length the number of requests it issues; this is one read for the page.
     *
     * <p>An application with no assessment is simply absent from the map. A draft has none
     * (AC-miniloan-035), and the queue must be able to say so rather than show a blank that looks
     * like a band.
     */
    @Transactional(readOnly = true)
    public Map<UUID, CreditAssessment.Band> bandsFor(List<LoanApplication> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = rows.stream().map(LoanApplication::getId).toList();
        return assessments.findByApplicationIdIn(ids).stream()
                .collect(Collectors.toMap(CreditAssessment::getApplicationId, CreditAssessment::getBand));
    }
}

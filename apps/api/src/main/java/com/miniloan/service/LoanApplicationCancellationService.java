package com.miniloan.service;

import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ยกเลิกใบสมัคร (UC-miniloan-007 · UC-miniloan-008 · API-009 · BR-miniloan-047@v1).
 *
 * <p>One route, two use cases, and which one applies is decided by the row rather than by the
 * caller's role alone — that is BR-miniloan-031@v2's whole point at AC-miniloan-092: <b>the right to
 * cancel moves from the supervisor to the assigned officer the moment the assignment happens, and
 * the two never hold it at once</b>. So the role is checked against what the application says about
 * itself, not before it is loaded:
 *
 * <ol>
 *   <li><b>state</b> — Disbursed has a loan account behind it and is answered with somewhere to go
 *       (AC-miniloan-048), while Rejected and Cancelled are simply over;
 *   <li><b>authority</b> — unassigned, the supervisor alone and only from Draft or Submitted
 *       (ACL-007 · AC-miniloan-090 · AC-miniloan-091); assigned, the officer it was given to alone
 *       (ACL-006 · AC-miniloan-092);
 *   <li><b>the reason</b> — BR-miniloan-047@v1, inside {@link LoanApplication#cancel} so nothing is
 *       written before it passes (AC-miniloan-068);
 *   <li>BR-miniloan-043@v1's key last, as approve and reject claim theirs.
 * </ol>
 *
 * <p><b>An UnderReview application nobody has been assigned yet can be cancelled by no one.</b>
 * ACL-007 stops the supervisor's scope at Submitted and ACL-006 gives the officer only what was
 * handed to them, so the matrix leaves that window closed — read literally, as a permission matrix
 * whose default is deny must be. No acceptance criterion covers it; it is reported rather than
 * papered over with a state message that would name the wrong reason.
 *
 * <p>The success sentence AC-miniloan-047 and AC-miniloan-090 quote is a fixed line on a screen, not
 * a decision this API makes — it belongs to the web unit that owns UI-miniloan-007/010, the way
 * every other rendered string does (REQ-miniloan-006).
 */
@Service
public class LoanApplicationCancellationService {

    /** FE-miniloan-002's mock scheme resolves one identity per role, so the role IS the staff id. */
    private static final String SUPERVISOR = "ROLE-003";

    private final LoanApplicationRepository applications;
    private final IdempotencyKeyRepository idempotencyKeys;

    public LoanApplicationCancellationService(
            LoanApplicationRepository applications, IdempotencyKeyRepository idempotencyKeys) {
        this.applications = applications;
        this.idempotencyKeys = idempotencyKeys;
    }

    public static class ApplicationNotFoundException extends RuntimeException {
        public ApplicationNotFoundException(UUID id) {
            super("ไม่พบใบสมัคร " + id);
        }
    }

    /** AC-miniloan-091 — refused at the API, not by a button the screen chose not to draw. */
    public static class SupervisorOnlyException extends RuntimeException {
        public SupervisorOnlyException() {
            super("ยกเลิกใบสมัครที่ยังไม่ถูกมอบหมายได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ");
        }
    }

    /** AC-miniloan-092 — the same person, the same application, one assignment later. */
    public static class AssignedOfficerOnlyException extends RuntimeException {
        public AssignedOfficerOnlyException() {
            super("ใบสมัครนี้ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะเจ้าหน้าที่ที่รับผิดชอบใบนี้");
        }
    }

    /**
     * The window the matrix leaves closed: UnderReview and not yet assigned. This is a permission
     * refusal and says so — a state message here would tell the supervisor the application is in
     * the wrong state, when the truth is that ACL-007's scope ends at Submitted.
     */
    public static class SupervisorScopeEndedException extends RuntimeException {
        public SupervisorScopeEndedException() {
            super(
                    "ยกเลิกไม่ได้ — สิทธิ์ยกเลิกของหัวหน้าครอบคลุมเฉพาะใบสมัครที่ยังไม่เข้าสู่การพิจารณา"
                            + " ใบนี้เข้าพิจารณาแล้วและยังไม่ถูกมอบหมาย");
        }
    }

    public static class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(UUID id) {
            super("ใบสมัคร " + id + " ถูกยกเลิกไปแล้ว — คำสั่งยกเลิกซ้ำถูกปฏิเสธ");
        }
    }

    @Transactional
    public LoanApplication cancel(UUID id, String reason, String actorId) {
        LoanApplication application =
                applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));

        if (!LoanApplication.CANCELLABLE.contains(application.getStatus())) {
            throw new LoanApplication.NotCancellableException(application);
        }
        requireAuthority(application, actorId);

        application.cancel(reason, actorId);
        claimCancelCommand(id);

        return applications.save(application);
    }

    /**
     * Only the four cancellable states reach here. An assignment is only ever made from UnderReview
     * ({@link LoanApplication#assignTo}), so an assigned application is UnderReview or something it
     * moved on to — never Draft or Submitted. That is why the assigned branch needs no second state
     * check: ACL-006's condition is already true of every row that can get there.
     */
    private void requireAuthority(LoanApplication application, String actorId) {
        String assignee = application.getAssignedLoanOfficerId();
        if (assignee == null) {
            if (!SUPERVISOR.equals(actorId)) {
                throw new SupervisorOnlyException();
            }
            if (application.getStatus() != LoanApplication.Status.Draft
                    && application.getStatus() != LoanApplication.Status.Submitted) {
                throw new SupervisorScopeEndedException();
            }
            return;
        }
        if (!assignee.equals(actorId)) {
            throw new AssignedOfficerOnlyException();
        }
    }

    private void claimCancelCommand(UUID applicationId) {
        try {
            idempotencyKeys.saveAndFlush(
                    new IdempotencyKey(
                            IdempotencyKey.CommandType.CancelApplication,
                            applicationId.toString(),
                            applicationId.toString()));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateCommandException(applicationId);
        }
    }
}

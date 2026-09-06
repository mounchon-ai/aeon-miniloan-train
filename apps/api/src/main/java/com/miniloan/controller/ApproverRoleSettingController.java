package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.ApproverRoleSetting;
import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.service.ApproverRoleSettingService;
import com.miniloan.service.ApproverRoleSettingService.ApproverRoleRequiredException;
import com.miniloan.service.ApproverRoleSettingService.LoanOfficerOnlyException;
import com.miniloan.service.ApproverRoleSettingService.SettingOutcome;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-019 (PUT /settings/closed-account-approver-role) และ API-022 (GET เส้นเดียวกัน) —
 * UC-miniloan-019 · ACL-017.
 *
 * <p><b>Both verbs answer to the one permission row.</b> ACL-017 grants ROLE-002 the use case and
 * {@code rbac.json} is default-deny, so Operations and Applicant are refused on the way in with
 * AC-miniloan-083's own sentence — the criterion measures the API refusal and the hidden menu
 * separately, and BR-miniloan-025@v1 is the reason the API one is not left to the screen.
 *
 * <p><b>PUT, not POST, and that is design's word</b> — API-019's operation line reads
 * {@code PUT /settings/closed-account-approver-role}. It fits what the route does: one setting with
 * one value, replaced whole, and a repeat of the same call leaves the same state behind. That is
 * also why no idempotency key rides along: ENT-012's {@code commandType} list names no setting
 * command, and a naturally idempotent write needs no dedup row to make it one.
 */
@RestController
@RequestMapping("/settings/closed-account-approver-role")
public class ApproverRoleSettingController {

    private final ApproverRoleSettingService settings;

    public ApproverRoleSettingController(ApproverRoleSettingService settings) {
        this.settings = settings;
    }

    /**
     * API-019. AC-miniloan-082 is the first time it is set and AC-miniloan-089 is a change; the
     * response carries the sentence for whichever one happened, because they are different sentences
     * in the criteria rather than one message with a variable in it.
     */
    @PutMapping
    public ResponseEntity<ApproverRoleSettingResponse> set(
            @RequestBody ApproverRoleRequest request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(
                ApproverRoleSettingResponse.from(settings.set(role, request.approverRole())));
    }

    /**
     * API-022. Before anybody has set it there is no row, and the answer is the setting with an empty
     * value rather than a 404 — BR-miniloan-040@v1's "ยังไม่ได้ตั้ง" is a state of the system, not a
     * missing resource, and a caller has to be able to see it in order to act on it.
     */
    @GetMapping
    public ResponseEntity<ApproverRoleSettingView> view(HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(
                settings
                        .view(role)
                        .map(ApproverRoleSettingView::from)
                        .orElseGet(ApproverRoleSettingView::unset));
    }

    // ── refusals ────────────────────────────────────────────────────────────

    /** BR-miniloan-048@v1 · AC-miniloan-083 — Operations and Applicant alike. */
    @ExceptionHandler(LoanOfficerOnlyException.class)
    public ResponseEntity<ErrorResponse> handleLoanOfficerOnly(LoanOfficerOnlyException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("APPROVER_ROLE_SETTING_LOAN_OFFICER_ONLY", ex.getMessage()));
    }

    @ExceptionHandler(ApproverRoleRequiredException.class)
    public ResponseEntity<ErrorResponse> handleRoleRequired(ApproverRoleRequiredException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("APPROVER_ROLE_REQUIRED", ex.getMessage()));
    }

    // ── wire shapes ─────────────────────────────────────────────────────────

    public record ApproverRoleRequest(ApproverRole approverRole) {}

    /** AC-miniloan-082 · AC-miniloan-089: the value now in force, and the sentence that says so. */
    public record ApproverRoleSettingResponse(
            String approverRole, String updatedBy, Instant updatedAt, String message) {

        static ApproverRoleSettingResponse from(SettingOutcome outcome) {
            ApproverRoleSetting setting = outcome.setting();
            return new ApproverRoleSettingResponse(
                    setting.getApproverRole().name(),
                    setting.getUpdatedBy(),
                    setting.getUpdatedAt(),
                    outcome.message());
        }
    }

    /** A null {@code approverRole} is BR-miniloan-040@v1's unset state, said out loud. */
    public record ApproverRoleSettingView(
            String approverRole, String updatedBy, Instant updatedAt) {

        static ApproverRoleSettingView from(ApproverRoleSetting setting) {
            return new ApproverRoleSettingView(
                    setting.approverRoleValue().map(Enum::name).orElse(null),
                    setting.getUpdatedBy(),
                    setting.getUpdatedAt());
        }

        static ApproverRoleSettingView unset() {
            return new ApproverRoleSettingView(null, null, null);
        }
    }
}

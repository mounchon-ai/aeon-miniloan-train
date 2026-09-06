package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.domain.ClosedAccountAdjustment.AdjustableField;
import com.miniloan.domain.LoanAccount;
import com.miniloan.repository.ApproverRoleSettingRepository;
import com.miniloan.repository.ClosedAccountAdjustmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.service.ApproverRoleSettingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BR-miniloan-025@v1 at the layer it names, for UC-miniloan-018.
 *
 * <p>AC-miniloan-079 measures the four-eyes refusal on both sides — "ทั้งจากหน้าจอและด้วยการเรียก
 * API อนุมัติโดยตรง" — and ACL-016's {@code enforceAt} lists {@code api} first. What only a test
 * over real HTTP can prove is that the four routes design declared EXIST at the paths it declared
 * them: a refusal arriving as a 404 would be the absence of a route rather than the enforcement of a
 * rule, and it would pass every service-level test unchanged.
 *
 * <p>{@link AuthTokenFilter} gates every request with no exemptions (FE-miniloan-002), so the token
 * rides on each call exactly as apps/web's interceptor attaches it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdjustmentControllerTest {

    private static final String LOAN_OFFICER = "ROLE-002";
    private static final String OPERATIONS = "ROLE-004";

    /** FE-miniloan-002's scheme — one identity per role. */
    private static final String SUPERVISOR_TOKEN = "Bearer mock-role-003";

    private static final String OPERATIONS_TOKEN = "Bearer mock-role-004";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApproverRoleSettingService approverSettings;
    @Autowired private ClosedAccountAdjustmentRepository adjustments;
    @Autowired private ApproverRoleSettingRepository approverRoles;
    @Autowired private LoanAccountRepository accounts;

    @BeforeEach
    void clean() {
        adjustments.deleteAll();
        approverRoles.deleteAll();
    }

    private LoanAccount closedAccount() {
        LoanAccount account =
                new LoanAccount(
                        UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000.00"), 12, OPERATIONS);
        account.reducePrincipal(account.getOutstandingPrincipal());
        account.close(LoanAccount.CloseReason.FullyPaid, Instant.parse("2026-08-31T04:00:00Z"));
        return accounts.save(account);
    }

    private ClosedAccountAdjustment filedBy(LoanAccount account, String requestedBy) {
        return adjustments.save(
                new ClosedAccountAdjustment(
                        account.getId(),
                        account.getId().toString(),
                        AdjustableField.LOAN_ACCOUNT_CLOSE_REASON,
                        "FullyPaid",
                        "EarlySettlement",
                        requestedBy,
                        Instant.parse("2026-09-01T02:00:00Z")));
    }

    /** API-017 · AC-miniloan-076, over real HTTP at the path design declared. */
    @Test
    void theApproverApprovesThroughTheApi() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();
        ClosedAccountAdjustment request = filedBy(account, OPERATIONS);

        mockMvc
                .perform(
                        post("/adjustments/{id}/approve", request.getId())
                                .header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjustment.status").value("Approved"))
                .andExpect(jsonPath("$.adjustment.approvedBy").value("ROLE-003"))
                .andExpect(jsonPath("$.adjustment.approvedAt").exists())
                .andExpect(jsonPath("$.adjustment.oldValue").value("FullyPaid"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ ")));

        assertThat(accounts.findById(account.getId()).orElseThrow().getCloseReason())
                .isEqualTo(LoanAccount.CloseReason.EarlySettlement);
    }

    /**
     * AC-miniloan-079 — the half that says "ด้วยการเรียก API อนุมัติโดยตรง". The approver role is
     * configured to Operations so the person who filed the request holds it too, and the API still
     * refuses with the criterion's own sentence.
     */
    @Test
    void approvingOnesOwnRequestIsRefusedByTheApiItself() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Operations);
        LoanAccount account = closedAccount();
        ClosedAccountAdjustment request = filedBy(account, OPERATIONS);

        mockMvc
                .perform(
                        post("/adjustments/{id}/approve", request.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADJUSTMENT_SELF_APPROVAL_REFUSED"))
                .andExpect(
                        jsonPath("$.message")
                                .value("อนุมัติคำขอของตัวเองไม่ได้ — ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้"));

        assertThat(adjustments.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(ClosedAccountAdjustment.Status.Pending);
        assertThat(accounts.findById(account.getId()).orElseThrow().getCloseReason())
                .isEqualTo(LoanAccount.CloseReason.FullyPaid);
    }

    /** ACL-016 · rbac.json is default-deny — a role the setting does not name reaches nothing. */
    @Test
    void aRoleThatIsNotTheConfiguredApproverIsRefusedByTheApi() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();
        ClosedAccountAdjustment request = filedBy(account, OPERATIONS);

        mockMvc
                .perform(
                        post("/adjustments/{id}/approve", request.getId())
                                .header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADJUSTMENT_NOT_THE_APPROVER"));
    }

    /** API-018 — the route exists and the old value stays. */
    @Test
    void theApproverRejectsThroughTheApi() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();
        ClosedAccountAdjustment request = filedBy(account, OPERATIONS);

        mockMvc
                .perform(
                        post("/adjustments/{id}/reject", request.getId())
                                .header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjustment.status").value("Rejected"))
                .andExpect(jsonPath("$.message").value("ปฏิเสธคำขอปรับปรุงแล้ว — ค่าเดิมของบัญชียังคงอยู่"));

        assertThat(accounts.findById(account.getId()).orElseThrow().getCloseReason())
                .isEqualTo(LoanAccount.CloseReason.FullyPaid);
    }

    /** API-024, called exactly as interfaces.json writes it: {@code GET /adjustments?status=Pending}. */
    @Test
    void thePendingQueueIsServedAtTheDeclaredQuery() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();
        ClosedAccountAdjustment request = filedBy(account, OPERATIONS);

        mockMvc
                .perform(
                        get("/adjustments").param("status", "Pending").header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(request.getId().toString()))
                .andExpect(jsonPath("$[0].fieldName").value("LoanAccount.closeReason"))
                .andExpect(jsonPath("$[0].approvedBy").doesNotExist());
    }

    /**
     * ACL-016's condition at the route: this queue is the Pending one, and a system-wide history of
     * decided adjustments is not something design has declared anywhere (GAP-miniloan-010).
     */
    @Test
    void theQueueRefusesToServeAnyOtherStatus() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);

        mockMvc
                .perform(
                        get("/adjustments")
                                .param("status", "Approved")
                                .header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ADJUSTMENT_QUEUE_PENDING_ONLY"));
    }

    /** API-025 — one request, carrying the approval record once there is one (AC-miniloan-084). */
    @Test
    void oneRequestIsServedWithItsApprovalRecord() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();
        ClosedAccountAdjustment request = filedBy(account, OPERATIONS);
        mockMvc
                .perform(
                        post("/adjustments/{id}/approve", request.getId())
                                .header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isOk());

        mockMvc
                .perform(get("/adjustments/{id}", request.getId()).header("Authorization", SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedBy").value("ROLE-004"))
                .andExpect(jsonPath("$.approvedBy").value("ROLE-003"))
                .andExpect(jsonPath("$.approvedAt").exists())
                .andExpect(jsonPath("$.oldValue").value("FullyPaid"))
                .andExpect(jsonPath("$.newValue").value("EarlySettlement"));
    }
}

package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.domain.LoanAccount;
import com.miniloan.repository.ApproverRoleSettingRepository;
import com.miniloan.repository.ClosedAccountAdjustmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.service.ApproverRoleSettingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BR-miniloan-025@v1 at the layer it names, for UC-miniloan-017.
 *
 * <p>Three criteria of this unit say "ด้วยการเรียก API ... โดยตรง" in as many words —
 * AC-miniloan-077 (editing a closed account), AC-miniloan-078 (deleting it) and AC-miniloan-081 (the
 * same edit with no approver named). {@code ClosedAccountAdjustmentServiceTest} measures the same
 * decisions at the domain layer; what only these can prove is that the routes EXIST — a refusal that
 * came back as a 404 would be the absence of a route rather than the enforcement of a rule, and it
 * would pass every service-level test unchanged.
 *
 * <p>{@link AuthTokenFilter} gates every request with no exemptions (FE-miniloan-002), so the token
 * rides on each call here exactly as apps/web's interceptor attaches it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClosedAccountAdjustmentControllerTest {

    private static final String OPERATIONS = "ROLE-004";
    private static final String LOAN_OFFICER = "ROLE-002";

    /** FE-miniloan-002's scheme — one identity per role. */
    private static final String OPERATIONS_TOKEN = "Bearer mock-role-004";

    private static final String APPLICANT_TOKEN = "Bearer mock-role-001";

    /**
     * ADR-006 — fieldName crosses the wire as the DECLARED name ENT-010 lists, and targetRecordId
     * names the row. The account id is substituted per test because a LoanAccount.* field may name
     * only the account being adjusted.
     */
    private static String body(java.util.UUID accountId) {
        return "{\"targetRecordId\":\""
                + accountId
                + "\",\"fieldName\":\"LoanAccount.closeReason\",\"oldValue\":\"FullyPaid\",\"newValue\":\"EarlySettlement\"}";
    }

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
        account.close(LoanAccount.CloseReason.FullyPaid, Instant.now());
        return accounts.save(account);
    }

    /** API-016 · AC-miniloan-076, over real HTTP at the path design declared. */
    @Test
    void operationsFilesTheRequestThroughTheApi() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/adjustments", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(account.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andExpect(jsonPath("$.approverRole").value("Supervisor"))
                .andExpect(jsonPath("$.targetRecordId").value(account.getId().toString()))
                .andExpect(jsonPath("$.fieldName").value("LoanAccount.closeReason"))
                .andExpect(jsonPath("$.oldValue").value("FullyPaid"))
                .andExpect(jsonPath("$.approvedBy").doesNotExist())
                .andExpect(
                        jsonPath("$.message")
                                .value("ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก Supervisor"));
    }

    /** ACL-015 · AC-miniloan-083's shape applied here: the API refuses, not just the screen. */
    @Test
    void anotherRoleIsRefusedByTheApiItself() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/adjustments", account.getId())
                                .header("Authorization", APPLICANT_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(account.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADJUSTMENT_OPERATIONS_ONLY"));

        assertThat(adjustments.count()).isZero();
    }

    /** AC-miniloan-080 — refused at the filing step, with no request created. */
    @Test
    void filingIsRefusedThroughTheApiWhenNoApproverIsSet() throws Exception {
        LoanAccount account = closedAccount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/adjustments", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(account.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPROVER_ROLE_NOT_SET"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "ยังไม่ได้ตั้ง role ผู้อนุมัติ — ใช้ฟีเจอร์แก้ข้อมูลบัญชีที่ปิดแล้วไม่ได้ · ให้ Loan Officer ตั้งค่า role ผู้อนุมัติก่อน"));

        assertThat(adjustments.count()).isZero();
    }

    /**
     * AC-miniloan-077, both halves: the direct edit is refused through the API, AND the request
     * already filed is still sitting at "รออนุมัติ" afterwards.
     */
    @Test
    void aDirectEditIsRefusedAndLeavesTheFiledRequestPending() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();
        mockMvc
                .perform(
                        post("/loan-accounts/{id}/adjustments", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(account.getId())))
                .andExpect(status().isCreated());

        mockMvc
                .perform(
                        patch("/loan-accounts/{id}", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"outstandingPrincipal\":\"0.00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_ACCOUNT_DIRECT_EDIT_REFUSED"))
                .andExpect(
                        jsonPath("$.message")
                                .value("แก้ข้อมูลบัญชีที่ปิดแล้วโดยตรงไม่ได้ — ต้องยื่นคำขอปรับปรุงและรออนุมัติก่อน"));

        List<ClosedAccountAdjustment> filed =
                adjustments.findByLoanAccountIdOrderByRequestedAtAsc(account.getId());
        assertThat(filed).hasSize(1);
        assertThat(filed.get(0).getStatus()).isEqualTo(ClosedAccountAdjustment.Status.Pending);
        assertThat(accounts.findById(account.getId()).orElseThrow().getOutstandingPrincipal())
                .isEqualByComparingTo("0.00");
    }

    /**
     * AC-miniloan-081 — the same route, the other sentence, and no adjustment left behind. There is
     * no "แก้แล้วรออนุมัติย้อนหลัง" state for a caller to walk into.
     */
    @Test
    void aDirectEditWithNoApproverSetIsRefusedWithItsOwnSentence() throws Exception {
        LoanAccount account = closedAccount();

        mockMvc
                .perform(
                        patch("/loan-accounts/{id}", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"outstandingPrincipal\":\"0.00\"}"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value("ยังไม่ได้ตั้ง role ผู้อนุมัติ — การแก้ไขมีผลก่อนได้รับอนุมัติไม่ได้ทุกกรณี"));

        assertThat(adjustments.count()).isZero();
    }

    /** AC-miniloan-078 — the account survives the shortcut, with its data intact. */
    @Test
    void deletingTheAccountIsRefusedAndTheAccountSurvives() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();

        mockMvc
                .perform(delete("/loan-accounts/{id}", account.getId()).header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_ACCOUNT_DELETION_REFUSED"))
                .andExpect(
                        jsonPath("$.message")
                                .value("ยกเลิกบัญชีสินเชื่อที่ปิดแล้วไม่ได้ — ถ้าข้อมูลผิด ให้ยื่นคำขอปรับปรุงเพื่อขออนุมัติแทน"));

        LoanAccount survived = accounts.findById(account.getId()).orElseThrow();
        assertThat(survived.getStatus()).isEqualTo(LoanAccount.Status.Closed);
        assertThat(survived.getPrincipalAmount()).isEqualByComparingTo("100000.00");
    }

    /**
     * ADR-006 — a fieldName outside ENT-010's five is refused by the contract before any rule runs.
     * The list is design's, and a value nobody declared cannot reach the service to be argued with.
     */
    @Test
    void aFieldNameOutsideTheDeclaredListIsRefused() throws Exception {
        approverSettings.set(LOAN_OFFICER, ApproverRole.Supervisor);
        LoanAccount account = closedAccount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/adjustments", account.getId())
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"targetRecordId\":\""
                                                + account.getId()
                                                + "\",\"fieldName\":\"LoanAccount.principalAmount\",\"oldValue\":\"1\",\"newValue\":\"2\"}"))
                .andExpect(status().is4xxClientError());

        assertThat(adjustments.count()).isZero();
    }

    /** BR-miniloan-030@v1 — the filter has no exemption list, and these three routes are not one. */
    @Test
    void noneOfTheThreeRoutesIsReachableWithoutAToken() throws Exception {
        LoanAccount account = closedAccount();

        mockMvc
                .perform(
                        post("/loan-accounts/{id}/adjustments", account.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(account.getId())))
                .andExpect(status().isUnauthorized());
        mockMvc
                .perform(
                        patch("/loan-accounts/{id}", account.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc
                .perform(delete("/loan-accounts/{id}", account.getId()))
                .andExpect(status().isUnauthorized());
    }
}

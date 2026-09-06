package com.miniloan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miniloan.repository.ApproverRoleSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BR-miniloan-025@v1 at the layer it names, for UC-miniloan-019.
 *
 * <p>AC-miniloan-083 measures the refusal on BOTH sides — "ทั้งจากหน้าจอและด้วยการเรียก API ตั้งค่า
 * โดยตรง" — and {@code ApproverRoleSettingServiceTest} only reaches the first. What only this can
 * prove is that API-019 and API-022 exist at the path and verb {@code interfaces.json} declares:
 * a wrong path or a POST where design wrote PUT puts a real caller on a 404, which is the ABSENCE of
 * a route rather than the enforcement of a rule, and it would pass every service-level test.
 *
 * <p>{@link AuthTokenFilter} gates every request with no exemptions (FE-miniloan-002), so the token
 * rides on each call here exactly as apps/web's interceptor attaches it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApproverRoleSettingControllerTest {

    private static final String PATH = "/settings/closed-account-approver-role";

    /** FE-miniloan-002's scheme — one identity per role. */
    private static final String LOAN_OFFICER_TOKEN = "Bearer mock-role-002";

    private static final String OPERATIONS_TOKEN = "Bearer mock-role-004";
    private static final String APPLICANT_TOKEN = "Bearer mock-role-001";

    private static final String SUPERVISOR_BODY = "{\"approverRole\":\"Supervisor\"}";
    private static final String OPERATIONS_BODY = "{\"approverRole\":\"Operations\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApproverRoleSettingRepository settings;

    @BeforeEach
    void clean() {
        settings.deleteAll();
    }

    /** API-019 · AC-miniloan-082, over real HTTP at the path and verb design declared. */
    @Test
    void loanOfficerSetsItThroughTheApi() throws Exception {
        mockMvc
                .perform(
                        put(PATH)
                                .header("Authorization", LOAN_OFFICER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(SUPERVISOR_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approverRole").value("Supervisor"))
                .andExpect(jsonPath("$.updatedBy").value("ROLE-002"))
                .andExpect(
                        jsonPath("$.message")
                                .value("ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น Supervisor เรียบร้อย"));
    }

    /** API-019 · AC-miniloan-089 — the second call is a change, and it says so. */
    @Test
    void changingItThroughTheApiSaysChanged() throws Exception {
        mockMvc
                .perform(
                        put(PATH)
                                .header("Authorization", LOAN_OFFICER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(SUPERVISOR_BODY))
                .andExpect(status().isOk());

        mockMvc
                .perform(
                        put(PATH)
                                .header("Authorization", LOAN_OFFICER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(OPERATIONS_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approverRole").value("Operations"))
                .andExpect(jsonPath("$.message").value("เปลี่ยน role ผู้อนุมัติเป็น Operations เรียบร้อย"));
    }

    /** AC-miniloan-083's API half, word for word — the caller never loads a screen. */
    @Test
    void operationsIsRefusedByTheApiItself() throws Exception {
        mockMvc
                .perform(
                        put(PATH)
                                .header("Authorization", OPERATIONS_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(OPERATIONS_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPROVER_ROLE_SETTING_LOAN_OFFICER_ONLY"))
                .andExpect(
                        jsonPath("$.message").value("ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer"));

        // "ค่า role ผู้อนุมัติไม่เปลี่ยน" — and nothing was created either.
        mockMvc
                .perform(get(PATH).header("Authorization", LOAN_OFFICER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approverRole").doesNotExist());
    }

    /** AC-miniloan-083 says the Applicant gives the same result. */
    @Test
    void applicantIsRefusedTheSameWay() throws Exception {
        mockMvc
                .perform(
                        put(PATH)
                                .header("Authorization", APPLICANT_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(SUPERVISOR_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPROVER_ROLE_SETTING_LOAN_OFFICER_ONLY"));
    }

    /**
     * API-022 before anybody has set it. BR-miniloan-040@v1's "ยังไม่ได้ตั้ง" is a state of the
     * system, so it answers 200 with no value rather than 404 with no explanation.
     */
    @Test
    void readingItBeforeItIsSetAnswersWithNoValue() throws Exception {
        mockMvc
                .perform(get(PATH).header("Authorization", LOAN_OFFICER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approverRole").doesNotExist());
    }

    /** API-022 after it is set, and gated by ACL-017 exactly as the write is. */
    @Test
    void readingItIsGatedByTheSameEntry() throws Exception {
        mockMvc
                .perform(
                        put(PATH)
                                .header("Authorization", LOAN_OFFICER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(SUPERVISOR_BODY))
                .andExpect(status().isOk());

        mockMvc
                .perform(get(PATH).header("Authorization", LOAN_OFFICER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approverRole").value("Supervisor"));

        mockMvc
                .perform(get(PATH).header("Authorization", OPERATIONS_TOKEN))
                .andExpect(status().isForbidden());
    }

    /** BR-miniloan-030@v1 — the filter has no exemption list, and these two routes are not one. */
    @Test
    void neitherRouteIsReachableWithoutAToken() throws Exception {
        mockMvc
                .perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(SUPERVISOR_BODY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }
}

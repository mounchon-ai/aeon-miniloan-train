package com.miniloan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API-021 (POST /credit-assessments/preview) at the layer ACL-021 names alongside the domain, for
 * UC-miniloan-028.
 *
 * <p><b>What only this can prove.</b> AC-miniloan-117 asks that the screen "เรียก API เพื่อขอตัวเลข
 * ทุกครั้ง" — a request the browser cannot make unless the route is registered at the path and verb
 * design declared. A wrong path answers 404, which is the ABSENCE of a route rather than the
 * enforcement of a rule, and {@code CreditPreviewServiceTest} would still be green;
 * {@code ContractOpenApiTest} (FE-miniloan-003) proves the springdoc mechanism against a test-only
 * fixture controller and enumerates no real route.
 *
 * <p>The figures asserted here are GD-miniloan-004's, so the number that leaves the API is the same
 * signed answer the approval path uses — the response carries {@code maxApprovableAmount} alone,
 * matching what {@code CreditAssessmentResponse} already hands a client.
 *
 * <p>{@link AuthTokenFilter} gates every request with no exemptions (FE-miniloan-002), so the token
 * rides on each call exactly as apps/web's interceptor attaches it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CreditPreviewControllerTest {

    private static final String PATH = "/credit-assessments/preview";

    /** FE-miniloan-002's scheme — one identity per role. */
    private static final String APPLICANT_TOKEN = "Bearer mock-role-001";

    private static final String OFFICER_TOKEN = "Bearer mock-role-002";

    @Autowired private MockMvc mockMvc;

    /** GD-miniloan-004 row 1 — 30,000.00 บาท of income, 150,000.00 บาท of ceiling. */
    @Test
    void theRouteServesTheGoldenAnswerForAnIncomeUnderTheCap() throws Exception {
        mockMvc
                .perform(
                        post(PATH)
                                .header("Authorization", APPLICANT_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monthlyIncome\":30000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxApprovableAmount").value(150000.00));
    }

    /** GD-miniloan-004 row 4 — 250,000.00 บาท of income is capped at 1,000,000.00 บาท. */
    @Test
    void theRouteServesTheGoldenAnswerWhenTheCapWins() throws Exception {
        mockMvc
                .perform(
                        post(PATH)
                                .header("Authorization", APPLICANT_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monthlyIncome\":250000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxApprovableAmount").value(1000000.00));
    }

    /** No application exists and none is created — API-021's "ยังไม่ต้องมีใบสมัครอยู่ในระบบ". */
    @Test
    void theRouteNeedsNoApplicationAndTheResponseCarriesOnlyTheCeiling() throws Exception {
        mockMvc
                .perform(
                        post(PATH)
                                .header("Authorization", APPLICANT_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monthlyIncome\":45678.33}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxApprovableAmount").value(228391.65))
                .andExpect(jsonPath("$.limitedBy").doesNotExist())
                .andExpect(jsonPath("$.formulaValue").doesNotExist());
    }

    /** A request with no income is malformed — 400, and it never reaches the calculation. */
    @Test
    void aRequestWithNoIncomeIsRefusedAsMalformed() throws Exception {
        mockMvc
                .perform(
                        post(PATH)
                                .header("Authorization", APPLICANT_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CreditPreviewController.INCOME_REQUIRED_CODE));
    }

    /** ACL-021's api half — default-deny, so a role with no entry is turned down by the route itself. */
    @Test
    void aRoleWithNoEntryIsRefusedByTheApiItself() throws Exception {
        mockMvc
                .perform(
                        post(PATH)
                                .header("Authorization", OFFICER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monthlyIncome\":30000.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CREDIT_PREVIEW_FORBIDDEN"));
    }

    /** BR-miniloan-030@v1 — the token filter runs before the route, with no exemption for this one. */
    @Test
    void anUnauthenticatedCallNeverReachesThePreview() throws Exception {
        mockMvc
                .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"monthlyIncome\":30000.00}"))
                .andExpect(status().isUnauthorized());
    }
}

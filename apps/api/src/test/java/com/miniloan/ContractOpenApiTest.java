package com.miniloan;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AC-miniloan-121/122 (BR-miniloan-029@v1): proves the OpenAPI contract springdoc serves at
 * /v3/api-docs is genuinely machine-testable — a real response is validated against its
 * declared schema (121, happy path), and a real mismatch is caught and names the field that
 * does not match (122), not silently accepted as "documentation, not enforced".
 *
 * <p>No business endpoint exists yet — FE-miniloan-004 is what adds one, and it does not
 * depend on this unit. {@link SampleContractController} exercises the exact mechanism every
 * future endpoint will be checked with; it is a nested class of this test file, compiled only
 * to target/test-classes and never part of the production jar. It is registered explicitly via
 * {@code @Import} rather than relying on component scanning to find a nested test class (it
 * does not, in practice — verified empirically, not assumed), so it is scoped to this test only
 * and never leaks into any other {@code @SpringBootTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ContractOpenApiTest.SampleContractController.class)
class ContractOpenApiTest {

    // AuthTokenFilter gates every endpoint, api-docs included (BR-miniloan-030@v1) — a plain
    // reading of "ด่านแรกของทุก endpoint" with no stated exception for the contract document
    // itself. If that should instead be public, that is a rule change for a person to make,
    // not something to decide inside this test.
    private static final String BEARER_TOKEN = "Bearer mock-role-001";

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void apiDocsServesAGenuineOpenApiDocument() {
        JsonNode doc = parse(getWithToken("/v3/api-docs").getBody());

        assertThat(doc.path("openapi").asText()).startsWith("3.");
        assertThat(doc.has("paths")).isTrue();
        assertThat(doc.has("components")).isTrue();
    }

    // AC-miniloan-121: a response matching its declared schema passes every field.
    @Test
    void aResponseMatchingItsDeclaredSchemaPassesValidation() {
        JsonSchema schema = fetchSampleContractSchema();
        JsonNode body = parse(getWithToken("/__contract-test/valid").getBody());

        Set<ValidationMessage> errors = schema.validate(body);

        assertThat(errors).isEmpty();
    }

    // AC-miniloan-122: a response that diverges from its declared schema fails validation, and
    // the failure names the field that does not match — not a silent pass.
    @Test
    void aResponseViolatingItsDeclaredSchemaFailsValidationAndNamesTheField() {
        JsonSchema schema = fetchSampleContractSchema();
        JsonNode body = parse(getWithToken("/__contract-test/invalid").getBody());

        Set<ValidationMessage> errors = schema.validate(body);

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.getMessage().contains("amount"));
    }

    private JsonSchema fetchSampleContractSchema() {
        JsonNode doc = parse(getWithToken("/v3/api-docs").getBody());
        JsonNode schemaNode = doc.path("components").path("schemas").path("SampleContractRecord");
        assertThat(schemaNode.isMissingNode())
                .as("springdoc must have documented SampleContractRecord")
                .isFalse();
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode);
    }

    private ResponseEntity<String> getWithToken(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", BEARER_TOKEN);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("response was not valid JSON: " + json, e);
        }
    }

    /** Test-only fixture. Flat primitive fields on purpose — keeps the extracted schema
     * self-contained with no $ref to resolve. */
    @RestController
    static class SampleContractController {

        @GetMapping(value = "/__contract-test/valid", produces = MediaType.APPLICATION_JSON_VALUE)
        SampleContractRecord valid() {
            return new SampleContractRecord("ok", new BigDecimal("1000.50"), true);
        }

        // Declares the SAME schema as /valid via @Schema(implementation=...), but the real
        // body deliberately violates it — "amount" is a JSON string where the schema says
        // number. This is exactly the bug AC-miniloan-122 describes: the real response no
        // longer matches what was declared.
        @Operation(
                responses =
                        @ApiResponse(
                                content =
                                        @Content(schema = @Schema(implementation = SampleContractRecord.class))))
        @GetMapping(value = "/__contract-test/invalid", produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<String> invalid() {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"label\":\"bad\",\"amount\":\"not-a-number\",\"active\":true}");
        }
    }

    record SampleContractRecord(String label, BigDecimal amount, boolean active) {}
}

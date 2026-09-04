package com.miniloan.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BR-miniloan-029@v1: the API must carry a testable contract. springdoc-openapi
 * (added in pom.xml) generates it automatically from real controllers/DTOs and
 * serves it at /v3/api-docs — this bean only supplies the document's metadata.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI miniloanOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("miniloan API")
                        .version("v1")
                        .description("Personal loan intake, assessment, approval, disbursement, "
                                + "repayment and closure. Every business rule is enforced here — "
                                + "apps/web calls this API for every decision."));
    }
}

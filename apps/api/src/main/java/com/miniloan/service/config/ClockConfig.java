package com.miniloan.service.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BR-miniloan-036@v1 decides which interest-rate version applies from the disbursement DATE, and
 * AC-miniloan-101 turns on a single day either side of an effective date. A test that cannot say
 * what day it is cannot measure that boundary, so the clock is a bean rather than a static call.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}

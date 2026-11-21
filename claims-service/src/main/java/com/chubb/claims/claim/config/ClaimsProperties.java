package com.chubb.claims.claim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

@ConfigurationProperties(prefix = "claims")
public record ClaimsProperties(
        int filingWindowDays,
        Map<String, BigDecimal> auditThresholds,
        Otp otp) {

    public record Otp(String stubCode) {
    }
}

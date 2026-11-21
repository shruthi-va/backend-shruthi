package com.chubb.claims.reporting.adapter.in.web.dto;

import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OutstandingClaimResponse(
        UUID claimId,
        String claimNumber,
        String market,
        String status,
        String claimType,
        BigDecimal claimedAmount,
        String currency,
        boolean auditFlag,
        Instant updatedAt) {

    public static OutstandingClaimResponse from(ClaimSnapshot s) {
        return new OutstandingClaimResponse(s.claimId(), s.claimNumber(), s.market(), s.status().name(),
                s.claimType(), s.claimedAmount(), s.currency(), s.auditFlag(), s.updatedAt());
    }
}

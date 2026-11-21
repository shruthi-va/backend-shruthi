package com.chubb.claims.reporting.application.query;

import java.math.BigDecimal;
import com.chubb.claims.reporting.domain.model.ClaimStatus;

/** One row of the outstanding summary, grouped by market, status, claim type and currency. */
public record OutstandingSummaryRow(
        String market,
        ClaimStatus status,
        String claimType,
        String currency,
        Long claimCount,
        BigDecimal totalClaimedAmount) {
}

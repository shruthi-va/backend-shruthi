package com.chubb.claims.reporting.adapter.in.web.dto;

import com.chubb.claims.reporting.application.query.OutstandingSummaryRow;
import java.math.BigDecimal;

public record OutstandingSummaryResponse(
        String market,
        String status,
        String claimType,
        String currency,
        long claimCount,
        BigDecimal totalClaimedAmount) {

    public static OutstandingSummaryResponse from(OutstandingSummaryRow row) {
        return new OutstandingSummaryResponse(row.market(), row.status().name(), row.claimType(), row.currency(),
                row.claimCount(), row.totalClaimedAmount());
    }
}

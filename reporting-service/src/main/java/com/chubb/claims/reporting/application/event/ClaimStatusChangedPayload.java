package com.chubb.claims.reporting.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body of claims.status-changed. Only the fields reporting needs are read; the
 * producer's owner and timestamp fields are ignored. No personal data is held.
 */
public record ClaimStatusChangedPayload(
        UUID claimId,
        String claimNumber,
        String market,
        String toStatus,
        String claimType,
        BigDecimal claimedAmount,
        String currency,
        boolean auditFlag) {
}

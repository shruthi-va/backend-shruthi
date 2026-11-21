package com.chubb.claims.reporting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Latest known state of one claim, as last reported by claims.status-changed.
 * {@code updatedAt} is the occurredAt of the event that produced this state.
 */
public record ClaimSnapshot(
        UUID claimId,
        String claimNumber,
        String market,
        ClaimStatus status,
        String claimType,
        BigDecimal claimedAmount,
        String currency,
        boolean auditFlag,
        Instant updatedAt) {
}

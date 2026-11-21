package com.chubb.claims.claim.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload of claims.status-changed. Carries no personal data (TDD 8.2).
 * The claim fields let reporting-service build its snapshot without calling claims-service.
 */
public record ClaimStatusChangedPayload(
        UUID claimId,
        String claimNumber,
        String market,
        String fromStatus,
        String toStatus,
        String ownerSubject,
        Instant changedAt,
        String claimType,
        BigDecimal claimedAmount,
        String currency,
        boolean auditFlag) {
}

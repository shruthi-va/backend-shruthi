package com.chubb.claims.claim.application.event;

import java.time.Instant;
import java.util.UUID;

/** Payload of claims.status-changed. Carries no personal data (TDD 8.2). */
public record ClaimStatusChangedPayload(
        UUID claimId,
        String claimNumber,
        String market,
        String fromStatus,
        String toStatus,
        String ownerSubject,
        Instant changedAt) {
}

package com.chubb.claims.claim.domain.model;

import java.time.Instant;
import java.util.UUID;

public record StatusChange(
        UUID claimId,
        ClaimStatus fromStatus,
        ClaimStatus toStatus,
        String actorSubject,
        Instant changedAt) {
}

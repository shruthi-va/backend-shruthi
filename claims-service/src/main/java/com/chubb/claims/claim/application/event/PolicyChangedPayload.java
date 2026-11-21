package com.chubb.claims.claim.application.event;

import java.util.UUID;

/** Mirrors policy-service's policies.changed payload. Read by the projection listener. */
public record PolicyChangedPayload(
        UUID policyId,
        String policyNumber,
        String market,
        String productType,
        String currency,
        String ownerSubject,
        String status,
        long revision) {
}

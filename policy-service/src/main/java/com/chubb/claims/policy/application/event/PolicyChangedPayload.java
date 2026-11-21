package com.chubb.claims.policy.application.event;

import java.util.UUID;

/** Payload of policies.changed. Carries no personal data. */
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

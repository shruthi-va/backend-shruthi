package com.chubb.claims.claim.domain.model;

import java.util.UUID;

/**
 * Local copy of the policy fields claims-service needs. Fed by policies.changed.
 */
public record PolicyProjection(
        UUID policyId,
        String policyNumber,
        String market,
        String currency,
        String ownerSubject,
        String status,
        long version) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isOwnedBy(String subject) {
        return ownerSubject != null && ownerSubject.equals(subject);
    }
}

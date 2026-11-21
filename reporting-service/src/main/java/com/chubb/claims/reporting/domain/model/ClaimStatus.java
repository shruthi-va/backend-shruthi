package com.chubb.claims.reporting.domain.model;

import java.util.EnumSet;
import java.util.Set;

/** Claim statuses published by claims-service. */
public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    PENDING_DECISION,
    APPROVED,
    REJECTED,
    SETTLED,
    WITHDRAWN;

    /** Outstanding means not terminal: REJECTED, SETTLED and WITHDRAWN are excluded. */
    public static Set<ClaimStatus> outstandingStatuses() {
        return EnumSet.of(SUBMITTED, UNDER_REVIEW, PENDING_DECISION, APPROVED);
    }
}

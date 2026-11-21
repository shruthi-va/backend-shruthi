package com.chubb.claims.claim.domain.model;

/**
 * Claim lifecycle. Transitions follow the lifecycle in docs/claim-lifecycle.md.
 */
public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    PENDING_DECISION,
    APPROVED,
    REJECTED,
    SETTLED,
    WITHDRAWN;

    public boolean canMoveTo(ClaimStatus next) {
        return switch (this) {
            case SUBMITTED -> next == UNDER_REVIEW || next == WITHDRAWN;
            case UNDER_REVIEW -> next == PENDING_DECISION;
            case PENDING_DECISION -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == SETTLED;
            case REJECTED, SETTLED, WITHDRAWN -> false;
        };
    }
}

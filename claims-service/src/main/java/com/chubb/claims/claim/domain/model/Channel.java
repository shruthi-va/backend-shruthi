package com.chubb.claims.claim.domain.model;

/**
 * How a claim entered the system. STAFF_RECORDED claims have no owner until the claimant verifies them.
 */
public enum Channel {
    ONLINE,
    STAFF_RECORDED
}

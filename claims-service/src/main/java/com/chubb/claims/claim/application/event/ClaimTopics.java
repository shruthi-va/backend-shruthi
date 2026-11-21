package com.chubb.claims.claim.application.event;

public final class ClaimTopics {

    public static final String STATUS_CHANGED = "claims.status-changed";
    public static final String RESERVE_CHANGED = "claims.reserve-changed";
    public static final String POLICY_CHANGED = "policies.changed";

    private ClaimTopics() {
    }
}

package com.chubb.claims.reporting.application.event;

/** Kafka topics this service consumes. Names match the claims-service producer. */
public final class ClaimTopics {

    public static final String STATUS_CHANGED = "claims.status-changed";

    private ClaimTopics() {
    }
}

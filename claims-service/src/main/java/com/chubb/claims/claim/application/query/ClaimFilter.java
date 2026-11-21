package com.chubb.claims.claim.application.query;

import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.ClaimType;

import java.time.Instant;

/**
 * Null fields are not filtered. Visibility (owner, market) is set by the service from the caller's role.
 */
public record ClaimFilter(
        String ownerSubject,
        String market,
        ClaimStatus status,
        ClaimType claimType,
        Instant filedFrom,
        Instant filedTo,
        int page,
        int size) {
}

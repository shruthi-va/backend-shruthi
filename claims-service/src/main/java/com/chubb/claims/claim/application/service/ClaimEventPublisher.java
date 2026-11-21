package com.chubb.claims.claim.application.service;

import com.chubb.claims.claim.application.event.ClaimStatusChangedPayload;
import com.chubb.claims.claim.application.event.ClaimTopics;
import com.chubb.claims.claim.application.event.ReserveChangedPayload;
import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.application.port.out.EventOutbox;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.StatusChange;
import com.chubb.claims.common.event.DomainEvent;
import org.springframework.stereotype.Component;

/**
 * Writes status history and queues events in the same transaction as the claim change (transactional outbox).
 */
@Component
class ClaimEventPublisher {

    static final String STATUS_EVENT_TYPE = "ClaimStatusChanged";
    static final String RESERVE_EVENT_TYPE = "ClaimReserveChanged";

    private final ClaimRepository claims;
    private final EventOutbox outbox;

    ClaimEventPublisher(ClaimRepository claims, EventOutbox outbox) {
        this.claims = claims;
        this.outbox = outbox;
    }

    void recordStatusChange(Claim claim, StatusChange change) {
        claims.appendHistory(change);
        var payload = new ClaimStatusChangedPayload(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getMarket(),
                change.fromStatus() == null ? null : change.fromStatus().name(),
                change.toStatus().name(),
                claim.getOwnerSubject(),
                change.changedAt(),
                claim.getClaimType().name(),
                claim.getClaimedAmount(),
                claim.getCurrency(),
                claim.isAuditFlag());
        outbox.append(ClaimTopics.STATUS_CHANGED, claim.getId().toString(),
                DomainEvent.of(STATUS_EVENT_TYPE, claim.getId().toString(), claim.getMarket(), payload));
    }

    void publishReserveChange(Claim claim, String action) {
        var payload = new ReserveChangedPayload(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getMarket(),
                claim.getCurrency(),
                claim.getProposedReserve(),
                claim.getApprovedReserve(),
                action);
        outbox.append(ClaimTopics.RESERVE_CHANGED, claim.getId().toString(),
                DomainEvent.of(RESERVE_EVENT_TYPE, claim.getId().toString(), claim.getMarket(), payload));
    }
}

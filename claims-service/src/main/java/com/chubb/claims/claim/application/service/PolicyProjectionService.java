package com.chubb.claims.claim.application.service;

import com.chubb.claims.claim.application.event.PolicyChangedPayload;
import com.chubb.claims.claim.application.port.out.PolicyProjectionRepository;
import com.chubb.claims.claim.application.port.out.ProcessedEventRepository;
import com.chubb.claims.claim.domain.model.PolicyProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Applies policies.changed to the local projection. Duplicate events are skipped, and an older revision never overwrites a newer one.
 */
@Service
public class PolicyProjectionService {

    private final PolicyProjectionRepository projections;
    private final ProcessedEventRepository processed;

    public PolicyProjectionService(PolicyProjectionRepository projections, ProcessedEventRepository processed) {
        this.projections = projections;
        this.processed = processed;
    }

    @Transactional
    public void apply(UUID eventId, PolicyChangedPayload payload) {
        if (processed.exists(eventId)) {
            return;
        }
        boolean newer = projections.findByPolicyId(payload.policyId())
                .map(current -> payload.revision() > current.version())
                .orElse(true);
        if (newer) {
            projections.save(new PolicyProjection(
                    payload.policyId(),
                    payload.policyNumber(),
                    payload.market(),
                    payload.currency(),
                    payload.ownerSubject(),
                    payload.status(),
                    payload.revision()));
        }
        processed.markProcessed(eventId);
    }
}

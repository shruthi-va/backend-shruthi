package com.chubb.claims.policy.adapter.in.web.dto;

import com.chubb.claims.policy.domain.model.Policy;
import com.chubb.claims.policy.domain.model.ProductType;
import java.time.Instant;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String policyNumber,
        String market,
        ProductType productType,
        String currency,
        String ownerSubject,
        String status,
        long revision,
        Instant updatedAt) {

    public static PolicyResponse from(Policy p) {
        return new PolicyResponse(p.id(), p.policyNumber(), p.market(), p.productType(), p.currency(),
                p.ownerSubject(), p.status().name(), p.revision(), p.updatedAt());
    }
}

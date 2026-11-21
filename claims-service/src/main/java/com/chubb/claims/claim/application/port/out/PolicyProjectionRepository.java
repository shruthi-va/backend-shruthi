package com.chubb.claims.claim.application.port.out;

import com.chubb.claims.claim.domain.model.PolicyProjection;

import java.util.Optional;
import java.util.UUID;

public interface PolicyProjectionRepository {

    Optional<PolicyProjection> findByPolicyId(UUID policyId);

    void save(PolicyProjection projection);
}

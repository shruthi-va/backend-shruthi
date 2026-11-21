package com.chubb.claims.claim.adapter.out.persistence;

import com.chubb.claims.claim.application.port.out.PolicyProjectionRepository;
import com.chubb.claims.claim.domain.model.PolicyProjection;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class PolicyProjectionPersistenceAdapter implements PolicyProjectionRepository {

    private final PolicyProjectionJpaRepository jpa;

    PolicyProjectionPersistenceAdapter(PolicyProjectionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PolicyProjection> findByPolicyId(UUID policyId) {
        return jpa.findById(policyId).map(PolicyProjectionEntity::toDomain);
    }

    @Override
    public void save(PolicyProjection projection) {
        jpa.save(new PolicyProjectionEntity(projection));
    }
}

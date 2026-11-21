package com.chubb.claims.policy.adapter.out.persistence;

import com.chubb.claims.policy.application.port.out.PolicyRepository;
import com.chubb.claims.policy.domain.model.Policy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class PolicyPersistenceAdapter implements PolicyRepository {

    private final PolicyJpaRepository jpa;

    PolicyPersistenceAdapter(PolicyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Policy save(Policy policy) {
        jpa.save(toEntity(policy));
        return policy;
    }

    @Override
    public Optional<Policy> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        return jpa.findByPolicyNumber(policyNumber).map(this::toDomain);
    }

    @Override
    public List<Policy> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Policy> findByMarket(String market) {
        return jpa.findByMarket(market).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Policy> findByOwnerSubject(String ownerSubject) {
        return jpa.findByOwnerSubject(ownerSubject).stream().map(this::toDomain).toList();
    }

    private PolicyEntity toEntity(Policy p) {
        return new PolicyEntity(p.id(), p.policyNumber(), p.market(), p.productType(), p.currency(),
                p.ownerSubject(), p.status(), p.revision(), p.updatedAt());
    }

    private Policy toDomain(PolicyEntity e) {
        return Policy.restore(e.getId(), e.getPolicyNumber(), e.getMarket(), e.getProductType(), e.getCurrency(),
                e.getOwnerSubject(), e.getStatus(), e.getVersion(), e.getUpdatedAt());
    }
}

package com.chubb.claims.policy.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PolicyJpaRepository extends JpaRepository<PolicyEntity, UUID> {

    Optional<PolicyEntity> findByPolicyNumber(String policyNumber);

    List<PolicyEntity> findByMarket(String market);

    List<PolicyEntity> findByOwnerSubject(String ownerSubject);
}

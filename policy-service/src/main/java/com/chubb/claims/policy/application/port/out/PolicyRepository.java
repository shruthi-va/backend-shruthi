package com.chubb.claims.policy.application.port.out;

import com.chubb.claims.policy.domain.model.Policy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository {

    Policy save(Policy policy);

    Optional<Policy> findById(UUID id);

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findAll();

    List<Policy> findByMarket(String market);

    List<Policy> findByOwnerSubject(String ownerSubject);
}

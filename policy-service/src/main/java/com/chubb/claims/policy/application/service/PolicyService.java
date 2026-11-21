package com.chubb.claims.policy.application.service;

import com.chubb.claims.common.error.BusinessRuleException;
import com.chubb.claims.common.error.ResourceNotFoundException;
import com.chubb.claims.common.event.DomainEvent;
import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.policy.application.command.CreatePolicyCommand;
import com.chubb.claims.policy.application.event.PolicyChangedPayload;
import com.chubb.claims.policy.application.port.out.EventOutbox;
import com.chubb.claims.policy.application.port.out.PolicyRepository;
import com.chubb.claims.policy.domain.model.Policy;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Policy catalogue. Managers create and link policies; staff read policies in their market;
 * claimants read only the policies they own.
 */
@Service
@Transactional
public class PolicyService {

    static final String TOPIC = "policies.changed";
    private static final String EVENT_TYPE = "PolicyChanged";

    private final PolicyRepository policies;
    private final EventOutbox outbox;

    public PolicyService(PolicyRepository policies, EventOutbox outbox) {
        this.policies = policies;
        this.outbox = outbox;
    }

    public Policy createPolicy(CurrentUser user, CreatePolicyCommand command) {
        requireManager(user);
        if (policies.findByPolicyNumber(command.policyNumber()).isPresent()) {
            throw new BusinessRuleException("Policy number already exists: " + command.policyNumber());
        }
        Policy policy = Policy.create(command.policyNumber(), command.market(), command.productType(), command.currency());
        policies.save(policy);
        publish(policy);
        return policy;
    }

    public Policy assignOwner(CurrentUser user, UUID policyId, String ownerSubject) {
        requireManager(user);
        Policy policy = load(policyId);
        policy.assignOwner(ownerSubject);
        policies.save(policy);
        publish(policy);
        return policy;
    }

    @Transactional(readOnly = true)
    public Policy getPolicy(CurrentUser user, UUID policyId) {
        Policy policy = load(policyId);
        if (!canView(user, policy)) {
            throw new ResourceNotFoundException("Policy not found: " + policyId);
        }
        return policy;
    }

    @Transactional(readOnly = true)
    public List<Policy> listPolicies(CurrentUser user) {
        if (user.isManager()) {
            return policies.findAll();
        }
        if (user.isStaff()) {
            return policies.findByMarket(user.market());
        }
        return policies.findByOwnerSubject(user.subject());
    }

    private Policy load(UUID policyId) {
        return policies.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
    }

    private boolean canView(CurrentUser user, Policy policy) {
        if (user.isManager()) {
            return true;
        }
        if (user.isStaff()) {
            return user.market() != null && user.market().equals(policy.market());
        }
        return user.subject().equals(policy.ownerSubject());
    }

    private void requireManager(CurrentUser user) {
        if (!user.isManager()) {
            throw new AccessDeniedException("Only managers can change policies");
        }
    }

    private void publish(Policy policy) {
        PolicyChangedPayload payload = new PolicyChangedPayload(
                policy.id(),
                policy.policyNumber(),
                policy.market(),
                policy.productType().name(),
                policy.currency(),
                policy.ownerSubject(),
                policy.status().name(),
                policy.revision());
        outbox.append(TOPIC, policy.id().toString(),
                DomainEvent.of(EVENT_TYPE, policy.id().toString(), policy.market(), payload));
    }
}

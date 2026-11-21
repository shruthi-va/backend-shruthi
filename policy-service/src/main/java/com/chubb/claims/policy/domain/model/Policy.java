package com.chubb.claims.policy.domain.model;

import com.chubb.claims.common.error.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;

/**
 * An insurance policy. The owner is the Keycloak subject of the claimant who holds it;
 * it is null until a manager links the policy to a claimant.
 */
public class Policy {

    private final UUID id;
    private final String policyNumber;
    private final String market;
    private final ProductType productType;
    private final String currency;
    private String ownerSubject;
    private PolicyStatus status;
    private long revision;
    private Instant updatedAt;

    private Policy(UUID id, String policyNumber, String market, ProductType productType, String currency,
                   String ownerSubject, PolicyStatus status, long revision, Instant updatedAt) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.market = market;
        this.productType = productType;
        this.currency = currency;
        this.ownerSubject = ownerSubject;
        this.status = status;
        this.revision = revision;
        this.updatedAt = updatedAt;
    }

    public static Policy create(String policyNumber, String market, ProductType productType, String currency) {
        return new Policy(UUID.randomUUID(), policyNumber, market, productType, currency,
                null, PolicyStatus.ACTIVE, 1L, Instant.now());
    }

    public static Policy restore(UUID id, String policyNumber, String market, ProductType productType,
                                 String currency, String ownerSubject, PolicyStatus status,
                                 long revision, Instant updatedAt) {
        return new Policy(id, policyNumber, market, productType, currency, ownerSubject, status, revision, updatedAt);
    }

    public void assignOwner(String subject) {
        if (ownerSubject != null && !ownerSubject.equals(subject)) {
            throw new BusinessRuleException("Policy " + policyNumber + " already belongs to another claimant");
        }
        if (subject.equals(ownerSubject)) {
            return;
        }
        ownerSubject = subject;
        revision++;
        updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public String policyNumber() { return policyNumber; }
    public String market() { return market; }
    public ProductType productType() { return productType; }
    public String currency() { return currency; }
    public String ownerSubject() { return ownerSubject; }
    public PolicyStatus status() { return status; }
    public long revision() { return revision; }
    public Instant updatedAt() { return updatedAt; }
}

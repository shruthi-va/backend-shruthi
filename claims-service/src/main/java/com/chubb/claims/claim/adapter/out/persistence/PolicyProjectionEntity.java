package com.chubb.claims.claim.adapter.out.persistence;

import com.chubb.claims.claim.domain.model.PolicyProjection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "policy_projection")
class PolicyProjectionEntity {

    @Id
    private UUID policyId;

    @Column(nullable = false)
    private String policyNumber;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String currency;

    private String ownerSubject;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private long version;

    protected PolicyProjectionEntity() {
    }

    PolicyProjectionEntity(PolicyProjection p) {
        this.policyId = p.policyId();
        this.policyNumber = p.policyNumber();
        this.market = p.market();
        this.currency = p.currency();
        this.ownerSubject = p.ownerSubject();
        this.status = p.status();
        this.version = p.version();
    }

    PolicyProjection toDomain() {
        return new PolicyProjection(policyId, policyNumber, market, currency, ownerSubject, status, version);
    }
}

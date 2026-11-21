package com.chubb.claims.policy.adapter.out.persistence;

import com.chubb.claims.policy.domain.model.PolicyStatus;
import com.chubb.claims.policy.domain.model.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy")
class PolicyEntity {

    @Id
    private UUID id;

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @Column(nullable = false)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(nullable = false)
    private String currency;

    @Column(name = "owner_subject")
    private String ownerSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PolicyEntity() {
    }

    PolicyEntity(UUID id, String policyNumber, String market, ProductType productType, String currency,
                 String ownerSubject, PolicyStatus status, long version, Instant updatedAt) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.market = market;
        this.productType = productType;
        this.currency = currency;
        this.ownerSubject = ownerSubject;
        this.status = status;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    UUID getId() { return id; }
    String getPolicyNumber() { return policyNumber; }
    String getMarket() { return market; }
    ProductType getProductType() { return productType; }
    String getCurrency() { return currency; }
    String getOwnerSubject() { return ownerSubject; }
    PolicyStatus getStatus() { return status; }
    long getVersion() { return version; }
    Instant getUpdatedAt() { return updatedAt; }
}

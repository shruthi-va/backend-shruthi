package com.chubb.claims.claim.adapter.out.persistence;

import com.chubb.claims.claim.domain.model.Channel;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.ClaimType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claim")
class ClaimEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String claimNumber;

    @Column(nullable = false)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private UUID policyId;

    @Column(nullable = false)
    private String policyNumber;

    private String ownerSubject;

    private UUID contactId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimType claimType;

    @Column(nullable = false)
    private LocalDate dateOfLoss;

    @Column(nullable = false)
    private Instant filedAt;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal claimedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(nullable = false)
    private boolean auditFlag;

    @Column(precision = 19, scale = 2)
    private BigDecimal proposedReserve;

    @Column(precision = 19, scale = 2)
    private BigDecimal approvedReserve;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ClaimEntity() {
    }

    static ClaimEntity from(Claim c) {
        ClaimEntity e = new ClaimEntity();
        e.id = c.getId();
        e.claimNumber = c.getClaimNumber();
        e.market = c.getMarket();
        e.channel = c.getChannel();
        e.policyId = c.getPolicyId();
        e.policyNumber = c.getPolicyNumber();
        e.contactId = c.getContactId();
        e.claimType = c.getClaimType();
        e.dateOfLoss = c.getDateOfLoss();
        e.filedAt = c.getFiledAt();
        e.currency = c.getCurrency();
        e.claimedAmount = c.getClaimedAmount();
        e.auditFlag = c.isAuditFlag();
        e.applyChanges(c);
        return e;
    }

    /** Copies the fields that change after filing. Immutable facts such as amount and policy are left alone. */
    void applyChanges(Claim c) {
        this.ownerSubject = c.getOwnerSubject();
        this.status = c.getStatus();
        this.proposedReserve = c.getProposedReserve();
        this.approvedReserve = c.getApprovedReserve();
        this.updatedAt = c.getUpdatedAt();
    }

    Claim toDomain() {
        return Claim.restore(id, claimNumber, market, channel, policyId, policyNumber, ownerSubject, contactId,
                claimType, dateOfLoss, filedAt, currency, claimedAmount, status, auditFlag, proposedReserve,
                approvedReserve, updatedAt);
    }

    UUID getId() { return id; }
}

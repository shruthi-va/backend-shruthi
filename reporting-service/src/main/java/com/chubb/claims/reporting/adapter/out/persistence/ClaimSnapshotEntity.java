package com.chubb.claims.reporting.adapter.out.persistence;

import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_snapshot")
class ClaimSnapshotEntity {

    @Id
    private UUID claimId;

    @Column(nullable = false)
    private String claimNumber;

    @Column(nullable = false)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(nullable = false)
    private String claimType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal claimedAmount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private boolean auditFlag;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ClaimSnapshotEntity() {
    }

    ClaimSnapshotEntity(ClaimSnapshot s) {
        this.claimId = s.claimId();
        this.claimNumber = s.claimNumber();
        this.market = s.market();
        this.status = s.status();
        this.claimType = s.claimType();
        this.claimedAmount = s.claimedAmount();
        this.currency = s.currency();
        this.auditFlag = s.auditFlag();
        this.updatedAt = s.updatedAt();
    }

    ClaimSnapshot toDomain() {
        return new ClaimSnapshot(claimId, claimNumber, market, status, claimType, claimedAmount, currency,
                auditFlag, updatedAt);
    }
}

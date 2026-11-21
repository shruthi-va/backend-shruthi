package com.chubb.claims.claim.adapter.out.persistence;

import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.StatusChange;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_status_history")
class ClaimHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID claimId;

    @Enumerated(EnumType.STRING)
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus toStatus;

    @Column(nullable = false)
    private String actorSubject;

    @Column(nullable = false)
    private Instant changedAt;

    protected ClaimHistoryEntity() {
    }

    ClaimHistoryEntity(StatusChange change) {
        this.claimId = change.claimId();
        this.fromStatus = change.fromStatus();
        this.toStatus = change.toStatus();
        this.actorSubject = change.actorSubject();
        this.changedAt = change.changedAt();
    }

    StatusChange toDomain() {
        return new StatusChange(claimId, fromStatus, toStatus, actorSubject, changedAt);
    }
}

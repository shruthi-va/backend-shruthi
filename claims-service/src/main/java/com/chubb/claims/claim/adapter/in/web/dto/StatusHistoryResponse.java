package com.chubb.claims.claim.adapter.in.web.dto;

import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.StatusChange;
import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID claimId,
        ClaimStatus fromStatus,
        ClaimStatus toStatus,
        String actorSubject,
        Instant changedAt) {

    public static StatusHistoryResponse from(StatusChange s) {
        return new StatusHistoryResponse(s.claimId(), s.fromStatus(), s.toStatus(), s.actorSubject(), s.changedAt());
    }
}

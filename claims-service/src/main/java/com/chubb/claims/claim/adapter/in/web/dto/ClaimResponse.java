package com.chubb.claims.claim.adapter.in.web.dto;

import com.chubb.claims.claim.domain.model.Channel;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.ClaimType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        String claimNumber,
        String market,
        Channel channel,
        UUID policyId,
        String policyNumber,
        String ownerSubject,
        UUID contactId,
        ClaimType claimType,
        LocalDate dateOfLoss,
        Instant filedAt,
        String currency,
        BigDecimal claimedAmount,
        ClaimStatus status,
        boolean auditFlag,
        BigDecimal proposedReserve,
        BigDecimal approvedReserve,
        Instant updatedAt) {

    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.getId(), c.getClaimNumber(), c.getMarket(), c.getChannel(), c.getPolicyId(),
                c.getPolicyNumber(), c.getOwnerSubject(), c.getContactId(), c.getClaimType(), c.getDateOfLoss(),
                c.getFiledAt(), c.getCurrency(), c.getClaimedAmount(), c.getStatus(), c.isAuditFlag(),
                c.getProposedReserve(), c.getApprovedReserve(), c.getUpdatedAt());
    }
}

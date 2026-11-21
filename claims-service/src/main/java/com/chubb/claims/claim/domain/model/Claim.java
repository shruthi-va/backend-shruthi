package com.chubb.claims.claim.domain.model;

import com.chubb.claims.common.error.BusinessRuleException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Claim {

    private final UUID id;
    private final String claimNumber;
    private final String market;
    private final Channel channel;
    private final UUID policyId;
    private final String policyNumber;
    private String ownerSubject;
    private final UUID contactId;
    private final ClaimType claimType;
    private final LocalDate dateOfLoss;
    private final Instant filedAt;
    private final String currency;
    private final BigDecimal claimedAmount;
    private ClaimStatus status;
    private final boolean auditFlag;
    private BigDecimal proposedReserve;
    private BigDecimal approvedReserve;
    private Instant updatedAt;

    private Claim(UUID id, String claimNumber, String market, Channel channel, UUID policyId, String policyNumber,
                  String ownerSubject, UUID contactId, ClaimType claimType, LocalDate dateOfLoss, Instant filedAt,
                  String currency, BigDecimal claimedAmount, ClaimStatus status, boolean auditFlag,
                  BigDecimal proposedReserve, BigDecimal approvedReserve, Instant updatedAt) {
        this.id = id;
        this.claimNumber = claimNumber;
        this.market = market;
        this.channel = channel;
        this.policyId = policyId;
        this.policyNumber = policyNumber;
        this.ownerSubject = ownerSubject;
        this.contactId = contactId;
        this.claimType = claimType;
        this.dateOfLoss = dateOfLoss;
        this.filedAt = filedAt;
        this.currency = currency;
        this.claimedAmount = claimedAmount;
        this.status = status;
        this.auditFlag = auditFlag;
        this.proposedReserve = proposedReserve;
        this.approvedReserve = approvedReserve;
        this.updatedAt = updatedAt;
    }

    public static Claim file(UUID id, String claimNumber, Channel channel, PolicyProjection policy,
                             String ownerSubject, UUID contactId, ClaimType claimType, LocalDate dateOfLoss,
                             BigDecimal claimedAmount, boolean auditFlag, Instant now) {
        if (claimedAmount.signum() <= 0) {
            throw new BusinessRuleException("Claimed amount must be greater than zero");
        }
        return new Claim(id, claimNumber, policy.market(), channel, policy.policyId(), policy.policyNumber(),
                ownerSubject, contactId, claimType, dateOfLoss, now, policy.currency(), claimedAmount,
                ClaimStatus.SUBMITTED, auditFlag, null, null, now);
    }

    public static Claim restore(UUID id, String claimNumber, String market, Channel channel, UUID policyId,
                                String policyNumber, String ownerSubject, UUID contactId, ClaimType claimType,
                                LocalDate dateOfLoss, Instant filedAt, String currency, BigDecimal claimedAmount,
                                ClaimStatus status, boolean auditFlag, BigDecimal proposedReserve,
                                BigDecimal approvedReserve, Instant updatedAt) {
        return new Claim(id, claimNumber, market, channel, policyId, policyNumber, ownerSubject, contactId,
                claimType, dateOfLoss, filedAt, currency, claimedAmount, status, auditFlag, proposedReserve,
                approvedReserve, updatedAt);
    }

    public StatusChange transitionTo(ClaimStatus next, String actorSubject, Instant now) {
        if (!status.canMoveTo(next)) {
            throw new BusinessRuleException("Cannot move claim from " + status + " to " + next);
        }
        StatusChange change = new StatusChange(id, status, next, actorSubject, now);
        status = next;
        updatedAt = now;
        return change;
    }

    /**
     * Option A phone verification: the claimant's OTP check has passed, so the claim now belongs to them.
     */
    public void assignOwner(String subject, Instant now) {
        if (channel != Channel.STAFF_RECORDED) {
            throw new BusinessRuleException("Only staff-recorded claims need verification");
        }
        if (ownerSubject != null) {
            throw new BusinessRuleException("Claim is already verified");
        }
        ownerSubject = subject;
        updatedAt = now;
    }

    public void proposeReserve(BigDecimal amount, Instant now) {
        requireReserveStage();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Reserve must be greater than zero");
        }
        proposedReserve = amount;
        updatedAt = now;
    }

    public void approveReserve(Instant now) {
        if (proposedReserve == null) {
            throw new BusinessRuleException("No proposed reserve to approve");
        }
        approvedReserve = proposedReserve;
        proposedReserve = null;
        updatedAt = now;
    }

    public void rejectReserve(Instant now) {
        if (proposedReserve == null) {
            throw new BusinessRuleException("No proposed reserve to reject");
        }
        proposedReserve = null;
        updatedAt = now;
    }

    private void requireReserveStage() {
        if (status != ClaimStatus.UNDER_REVIEW && status != ClaimStatus.PENDING_DECISION) {
            throw new BusinessRuleException("Reserves can only change while the claim is under review");
        }
    }

    public boolean isOwnedBy(String subject) {
        return ownerSubject != null && ownerSubject.equals(subject);
    }

    public UUID getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public String getMarket() { return market; }
    public Channel getChannel() { return channel; }
    public UUID getPolicyId() { return policyId; }
    public String getPolicyNumber() { return policyNumber; }
    public String getOwnerSubject() { return ownerSubject; }
    public UUID getContactId() { return contactId; }
    public ClaimType getClaimType() { return claimType; }
    public LocalDate getDateOfLoss() { return dateOfLoss; }
    public Instant getFiledAt() { return filedAt; }
    public String getCurrency() { return currency; }
    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public ClaimStatus getStatus() { return status; }
    public boolean isAuditFlag() { return auditFlag; }
    public BigDecimal getProposedReserve() { return proposedReserve; }
    public BigDecimal getApprovedReserve() { return approvedReserve; }
    public Instant getUpdatedAt() { return updatedAt; }
}

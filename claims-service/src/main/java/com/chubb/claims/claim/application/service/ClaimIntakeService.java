package com.chubb.claims.claim.application.service;

import com.chubb.claims.claim.application.command.FileClaimCommand;
import com.chubb.claims.claim.application.command.StaffRecordClaimCommand;
import com.chubb.claims.claim.application.command.VerifyClaimCommand;
import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.application.port.out.OtpVerifier;
import com.chubb.claims.claim.application.port.out.PolicyProjectionRepository;
import com.chubb.claims.claim.config.ClaimsProperties;
import com.chubb.claims.claim.domain.model.Channel;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.ClaimType;
import com.chubb.claims.claim.domain.model.PolicyProjection;
import com.chubb.claims.claim.domain.model.StatusChange;
import com.chubb.claims.claim.domain.rules.ClaimRules;
import com.chubb.claims.common.error.BusinessRuleException;
import com.chubb.claims.common.error.ResourceNotFoundException;
import com.chubb.claims.common.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClaimIntakeService {

    private final ClaimRepository claims;
    private final PolicyProjectionRepository projections;
    private final OtpVerifier otpVerifier;
    private final ClaimsProperties properties;
    private final ClaimEventPublisher events;
    private final Clock clock;

    public ClaimIntakeService(ClaimRepository claims, PolicyProjectionRepository projections,
                              OtpVerifier otpVerifier, ClaimsProperties properties,
                              ClaimEventPublisher events, Clock clock) {
        this.claims = claims;
        this.projections = projections;
        this.otpVerifier = otpVerifier;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Claim fileOnline(CurrentUser user, FileClaimCommand cmd) {
        ClaimAccess.requireRole(user.isClaimant(), "Only claimants can file online");
        PolicyProjection policy = projections.findByPolicyId(cmd.policyId())
                .orElseThrow(() -> new BusinessRuleException("Policy is not yet available for claims"));
        if (!policy.isOwnedBy(user.subject())) {
            throw new ResourceNotFoundException("Policy", cmd.policyId());
        }
        requireActive(policy);
        return file(user, Channel.ONLINE, policy, user.subject(), null, cmd.claimType(), cmd.dateOfLoss(),
                cmd.claimedAmount());
    }

    @Transactional
    public Claim staffRecord(CurrentUser user, StaffRecordClaimCommand cmd) {
        ClaimAccess.requireRole(user.isStaff(), "Only claims staff can record phone claims");
        PolicyProjection policy = projections.findByPolicyId(cmd.policyId())
                .orElseThrow(() -> new BusinessRuleException("Policy is not yet available for claims"));
        if (!policy.market().equals(user.market())) {
            throw new ResourceNotFoundException("Policy", cmd.policyId());
        }
        requireActive(policy);
        return file(user, Channel.STAFF_RECORDED, policy, null, cmd.contactId(), cmd.claimType(),
                cmd.dateOfLoss(), cmd.claimedAmount());
    }

    /**
     * Option A: the claimant proves ownership with the policy number and an OTP, then the claim is linked to them.
     */
    @Transactional
    public Claim verify(CurrentUser user, UUID claimId, VerifyClaimCommand cmd) {
        ClaimAccess.requireRole(user.isClaimant(), "Only claimants can verify a phone claim");
        Claim claim = claims.findById(claimId).orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));
        boolean policyMatches = claim.getPolicyNumber().equals(cmd.policyNumber());
        if (!policyMatches || !otpVerifier.verify(cmd.policyNumber(), cmd.otp())) {
            throw new BusinessRuleException("Verification failed");
        }
        Instant now = clock.instant();
        claim.assignOwner(user.subject(), now);
        return claims.save(claim);
    }

    private Claim file(CurrentUser user, Channel channel, PolicyProjection policy, String ownerSubject,
                       UUID contactId, ClaimType claimType,
                       LocalDate dateOfLoss, BigDecimal claimedAmount) {
        Instant now = clock.instant();
        LocalDate filedOn = LocalDate.ofInstant(now, ZoneOffset.UTC);
        ClaimRules.requireFilingWindow(dateOfLoss, filedOn, properties.filingWindowDays());

        Optional<BigDecimal> threshold = Optional.ofNullable(properties.auditThresholds())
                .map(map -> map.get(policy.currency()));
        boolean auditFlag = ClaimRules.requiresAudit(claimedAmount, threshold);

        UUID id = UUID.randomUUID();
        Claim claim = Claim.file(id, newClaimNumber(), channel, policy, ownerSubject, contactId, claimType,
                dateOfLoss, claimedAmount, auditFlag, now);
        Claim saved = claims.save(claim);
        events.recordStatusChange(saved,
                new StatusChange(id, null, ClaimStatus.SUBMITTED, user.subject(), now));
        return saved;
    }

    private void requireActive(PolicyProjection policy) {
        if (!policy.isActive()) {
            throw new BusinessRuleException("Policy is not active");
        }
    }

    private static String newClaimNumber() {
        return "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

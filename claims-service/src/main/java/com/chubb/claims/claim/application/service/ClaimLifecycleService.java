package com.chubb.claims.claim.application.service;

import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.StatusChange;
import com.chubb.claims.common.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Staff move a claim through review and recommendation. Managers decide, and staff or managers settle.
 */
@Service
public class ClaimLifecycleService {

    private static final String ACTION_PROPOSED = "PROPOSED";
    private static final String ACTION_APPROVED = "APPROVED";
    private static final String ACTION_REJECTED = "REJECTED";

    private final ClaimRepository claims;
    private final ClaimAccess access;
    private final ClaimEventPublisher events;
    private final Clock clock;

    public ClaimLifecycleService(ClaimRepository claims, ClaimAccess access, ClaimEventPublisher events, Clock clock) {
        this.claims = claims;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Claim startReview(CurrentUser user, UUID claimId) {
        ClaimAccess.requireRole(user.isStaff(), "Only claims staff can start a review");
        return move(access.loadVisible(user, claimId), ClaimStatus.UNDER_REVIEW, user);
    }

    @Transactional
    public Claim recommend(CurrentUser user, UUID claimId) {
        ClaimAccess.requireRole(user.isStaff(), "Only claims staff can recommend a claim");
        return move(access.loadVisible(user, claimId), ClaimStatus.PENDING_DECISION, user);
    }

    @Transactional
    public Claim decide(CurrentUser user, UUID claimId, boolean approve) {
        ClaimAccess.requireRole(user.isManager(), "Only managers can decide a claim");
        ClaimStatus next = approve ? ClaimStatus.APPROVED : ClaimStatus.REJECTED;
        return move(access.loadVisible(user, claimId), next, user);
    }

    @Transactional
    public Claim settle(CurrentUser user, UUID claimId) {
        ClaimAccess.requireRole(user.isStaff() || user.isManager(), "Only staff or managers can settle a claim");
        return move(access.loadVisible(user, claimId), ClaimStatus.SETTLED, user);
    }

    @Transactional
    public Claim withdraw(CurrentUser user, UUID claimId) {
        ClaimAccess.requireRole(user.isClaimant(), "Only claimants can withdraw a claim");
        return move(access.loadVisible(user, claimId), ClaimStatus.WITHDRAWN, user);
    }

    @Transactional
    public Claim proposeReserve(CurrentUser user, UUID claimId, BigDecimal amount) {
        ClaimAccess.requireRole(user.isStaff(), "Only claims staff can propose a reserve");
        Claim claim = access.loadVisible(user, claimId);
        claim.proposeReserve(amount, clock.instant());
        return saveReserve(claim, ACTION_PROPOSED);
    }

    @Transactional
    public Claim approveReserve(CurrentUser user, UUID claimId) {
        ClaimAccess.requireRole(user.isManager(), "Only managers can approve a reserve");
        Claim claim = access.loadVisible(user, claimId);
        claim.approveReserve(clock.instant());
        return saveReserve(claim, ACTION_APPROVED);
    }

    @Transactional
    public Claim rejectReserve(CurrentUser user, UUID claimId) {
        ClaimAccess.requireRole(user.isManager(), "Only managers can reject a reserve");
        Claim claim = access.loadVisible(user, claimId);
        claim.rejectReserve(clock.instant());
        return saveReserve(claim, ACTION_REJECTED);
    }

    private Claim move(Claim claim, ClaimStatus next, CurrentUser actor) {
        StatusChange change = claim.transitionTo(next, actor.subject(), clock.instant());
        Claim saved = claims.save(claim);
        events.recordStatusChange(saved, change);
        return saved;
    }

    private Claim saveReserve(Claim claim, String action) {
        Claim saved = claims.save(claim);
        events.publishReserveChange(saved, action);
        return saved;
    }
}

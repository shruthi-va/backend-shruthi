package com.chubb.claims.claim.application.service;

import static com.chubb.claims.claim.ClaimTestData.CLAIMANT_A;
import static com.chubb.claims.claim.ClaimTestData.CLOCK;
import static com.chubb.claims.claim.ClaimTestData.MANAGER;
import static com.chubb.claims.claim.ClaimTestData.NOW;
import static com.chubb.claims.claim.ClaimTestData.SG_STAFF;
import static com.chubb.claims.claim.ClaimTestData.onlineClaim;
import static com.chubb.claims.claim.ClaimTestData.policy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.application.port.out.EventOutbox;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.common.error.BusinessRuleException;
import com.chubb.claims.common.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ClaimLifecycleServiceTest {

    private static final BigDecimal AMOUNT = new BigDecimal("500000");
    private static final BigDecimal RESERVE = new BigDecimal("250000");

    @Mock
    private ClaimRepository claims;

    @Mock
    private EventOutbox outbox;

    private ClaimLifecycleService service;

    @BeforeEach
    void setUp() {
        ClaimEventPublisher events = new ClaimEventPublisher(claims, outbox);
        service = new ClaimLifecycleService(claims, new ClaimAccess(claims), events, CLOCK);
    }

    @Test
    void staffStartReviewMovesClaimAndPublishesStatusChange() {
        Claim claim = submittedClaim("SG");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim reviewed = service.startReview(SG_STAFF, claim.getId());

        assertThat(reviewed.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        verify(claims).appendHistory(argThat(change -> change.toStatus() == ClaimStatus.UNDER_REVIEW
                && "staff-sg".equals(change.actorSubject())));
        verify(outbox).append(eq("claims.status-changed"), anyString(), any());
    }

    @Test
    void skippingReviewStepIsRejectedWithoutSaveOrPublish() {
        Claim claim = submittedClaim("SG");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.recommend(SG_STAFF, claim.getId()))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        verify(claims, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void managerCannotStartReview() {
        assertThatThrownBy(() -> service.startReview(MANAGER, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(claims);
    }

    @Test
    void staffFromAnotherMarketGetsNotFound() {
        Claim claim = submittedClaim("HK");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.startReview(SG_STAFF, claim.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(claims, never()).save(any());
    }

    @Test
    void claimantCannotDecide() {
        assertThatThrownBy(() -> service.decide(CLAIMANT_A, UUID.randomUUID(), true))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(claims);
    }

    @Test
    void managerApprovesRecommendedClaim() {
        Claim claim = pendingDecisionClaim("SG");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim decided = service.decide(MANAGER, claim.getId(), true);

        assertThat(decided.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        verify(outbox).append(eq("claims.status-changed"), anyString(), any());
    }

    @Test
    void staffProposesReserveWhileUnderReviewAndPublishesChange() {
        Claim claim = underReviewClaim("SG");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim proposed = service.proposeReserve(SG_STAFF, claim.getId(), RESERVE);

        assertThat(proposed.getProposedReserve()).isEqualTo(RESERVE);
        verify(outbox).append(eq("claims.reserve-changed"), anyString(), any());
    }

    @Test
    void managerCannotProposeReserve() {
        assertThatThrownBy(() -> service.proposeReserve(MANAGER, UUID.randomUUID(), RESERVE))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(claims);
    }

    @Test
    void approvingReserveWithoutProposalIsRejected() {
        Claim claim = underReviewClaim("SG");
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.approveReserve(MANAGER, claim.getId()))
                .isInstanceOf(BusinessRuleException.class);

        verify(claims, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void managerApprovesProposedReserve() {
        Claim claim = underReviewClaim("SG");
        claim.proposeReserve(RESERVE, NOW);
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim approved = service.approveReserve(MANAGER, claim.getId());

        assertThat(approved.getApprovedReserve()).isEqualTo(RESERVE);
        assertThat(approved.getProposedReserve()).isNull();
        verify(outbox).append(eq("claims.reserve-changed"), anyString(), any());
    }

    private static Claim submittedClaim(String market) {
        return onlineClaim(policy(market, "SGD", CLAIMANT_A.subject()), AMOUNT);
    }

    private static Claim underReviewClaim(String market) {
        Claim claim = submittedClaim(market);
        claim.transitionTo(ClaimStatus.UNDER_REVIEW, "staff-sg", NOW);
        return claim;
    }

    private static Claim pendingDecisionClaim(String market) {
        Claim claim = underReviewClaim(market);
        claim.transitionTo(ClaimStatus.PENDING_DECISION, "staff-sg", NOW);
        return claim;
    }
}

package com.chubb.claims.claim.application.service;

import static com.chubb.claims.claim.ClaimTestData.CLAIMANT_A;
import static com.chubb.claims.claim.ClaimTestData.CLAIMANT_B;
import static com.chubb.claims.claim.ClaimTestData.CLOCK;
import static com.chubb.claims.claim.ClaimTestData.HK_STAFF;
import static com.chubb.claims.claim.ClaimTestData.LOSS_DATE;
import static com.chubb.claims.claim.ClaimTestData.PROPERTIES;
import static com.chubb.claims.claim.ClaimTestData.SG_STAFF;
import static com.chubb.claims.claim.ClaimTestData.MANAGER;
import static com.chubb.claims.claim.ClaimTestData.policy;
import static com.chubb.claims.claim.ClaimTestData.staffRecordedClaim;
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

import com.chubb.claims.claim.application.command.FileClaimCommand;
import com.chubb.claims.claim.application.command.StaffRecordClaimCommand;
import com.chubb.claims.claim.application.command.VerifyClaimCommand;
import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.application.port.out.EventOutbox;
import com.chubb.claims.claim.application.port.out.OtpVerifier;
import com.chubb.claims.claim.application.port.out.PolicyProjectionRepository;
import com.chubb.claims.claim.domain.model.Channel;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.ClaimType;
import com.chubb.claims.claim.domain.model.PolicyProjection;
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
class ClaimIntakeServiceTest {

    private static final BigDecimal SMALL_CLAIM = new BigDecimal("500000");

    @Mock
    private ClaimRepository claims;

    @Mock
    private PolicyProjectionRepository projections;

    @Mock
    private OtpVerifier otpVerifier;

    @Mock
    private EventOutbox outbox;

    private ClaimIntakeService service;

    @BeforeEach
    void setUp() {
        ClaimEventPublisher events = new ClaimEventPublisher(claims, outbox);
        service = new ClaimIntakeService(claims, projections, otpVerifier, PROPERTIES, events, CLOCK);
    }

    @Test
    void onlineFilingCreatesSubmittedClaimOwnedByClaimantAndPublishesStatus() {
        PolicyProjection policy = policy("SG", "SGD", CLAIMANT_A.subject());
        when(projections.findByPolicyId(policy.policyId())).thenReturn(Optional.of(policy));
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim filed = service.fileOnline(CLAIMANT_A,
                new FileClaimCommand(policy.policyId(), ClaimType.MOTOR, LOSS_DATE, SMALL_CLAIM));

        assertThat(filed.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(filed.getChannel()).isEqualTo(Channel.ONLINE);
        assertThat(filed.getOwnerSubject()).isEqualTo("claimant-a");
        assertThat(filed.getClaimNumber()).startsWith("CLM-");
        assertThat(filed.isAuditFlag()).isFalse();
        verify(claims).appendHistory(argThat(change -> change.fromStatus() == null
                && change.toStatus() == ClaimStatus.SUBMITTED));
        verify(outbox).append(eq("claims.status-changed"), anyString(), any());
    }

    @Test
    void claimAtOrAboveAuditThresholdIsFlagged() {
        PolicyProjection policy = policy("SG", "SGD", CLAIMANT_A.subject());
        when(projections.findByPolicyId(policy.policyId())).thenReturn(Optional.of(policy));
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim filed = service.fileOnline(CLAIMANT_A,
                new FileClaimCommand(policy.policyId(), ClaimType.MOTOR, LOSS_DATE, new BigDecimal("1000000")));

        assertThat(filed.isAuditFlag()).isTrue();
    }

    @Test
    void claimantCannotFileAgainstAPolicyOwnedByAnotherClaimant() {
        PolicyProjection policy = policy("SG", "SGD", CLAIMANT_B.subject());
        when(projections.findByPolicyId(policy.policyId())).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> service.fileOnline(CLAIMANT_A,
                new FileClaimCommand(policy.policyId(), ClaimType.MOTOR, LOSS_DATE, SMALL_CLAIM)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(claims, never()).save(any());
    }

    @Test
    void lossOutsideFilingWindowIsRejected() {
        PolicyProjection policy = policy("SG", "SGD", CLAIMANT_A.subject());
        when(projections.findByPolicyId(policy.policyId())).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> service.fileOnline(CLAIMANT_A,
                new FileClaimCommand(policy.policyId(), ClaimType.MOTOR, LOSS_DATE.minusDays(40), SMALL_CLAIM)))
                .isInstanceOf(BusinessRuleException.class);

        verify(claims, never()).save(any());
    }

    @Test
    void staffCannotFileOnlineClaims() {
        assertThatThrownBy(() -> service.fileOnline(SG_STAFF,
                new FileClaimCommand(UUID.randomUUID(), ClaimType.MOTOR, LOSS_DATE, SMALL_CLAIM)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(projections, claims);
    }

    @Test
    void staffCannotRecordPhoneClaimForAnotherMarketsPolicy() {
        PolicyProjection hkPolicy = policy("HK", "HKD", null);
        when(projections.findByPolicyId(hkPolicy.policyId())).thenReturn(Optional.of(hkPolicy));

        assertThatThrownBy(() -> service.staffRecord(SG_STAFF, new StaffRecordClaimCommand(
                hkPolicy.policyId(), UUID.randomUUID(), ClaimType.MOTOR, LOSS_DATE, SMALL_CLAIM)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(claims, never()).save(any());
    }

    @Test
    void staffRecordedClaimHasNoOwnerUntilVerified() {
        PolicyProjection policy = policy("HK", "HKD", null);
        when(projections.findByPolicyId(policy.policyId())).thenReturn(Optional.of(policy));
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim recorded = service.staffRecord(HK_STAFF, new StaffRecordClaimCommand(
                policy.policyId(), UUID.randomUUID(), ClaimType.PROPERTY, LOSS_DATE, SMALL_CLAIM));

        assertThat(recorded.getChannel()).isEqualTo(Channel.STAFF_RECORDED);
        assertThat(recorded.getOwnerSubject()).isNull();
        assertThat(recorded.getContactId()).isNotNull();
    }

    @Test
    void claimantCannotRecordPhoneClaims() {
        assertThatThrownBy(() -> service.staffRecord(CLAIMANT_A, new StaffRecordClaimCommand(
                UUID.randomUUID(), UUID.randomUUID(), ClaimType.MOTOR, LOSS_DATE, SMALL_CLAIM)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(projections, claims);
    }

    @Test
    void wrongOtpLeavesPhoneClaimUnowned() {
        Claim claim = staffRecordedClaim(policy("SG", "SGD", null), SMALL_CLAIM);
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(otpVerifier.verify("POL-1", "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.verify(CLAIMANT_A, claim.getId(), new VerifyClaimCommand("POL-1", "000000")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(claim.getOwnerSubject()).isNull();
        verify(claims, never()).save(any());
    }

    @Test
    void wrongPolicyNumberIsRejectedWithoutCheckingOtp() {
        Claim claim = staffRecordedClaim(policy("SG", "SGD", null), SMALL_CLAIM);
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.verify(CLAIMANT_A, claim.getId(), new VerifyClaimCommand("POL-9", "123456")))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(otpVerifier);
    }

    @Test
    void matchingPolicyAndOtpLinksClaimToClaimant() {
        Claim claim = staffRecordedClaim(policy("SG", "SGD", null), SMALL_CLAIM);
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(otpVerifier.verify("POL-1", "123456")).thenReturn(true);
        when(claims.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim verified = service.verify(CLAIMANT_A, claim.getId(), new VerifyClaimCommand("POL-1", "123456"));

        assertThat(verified.getOwnerSubject()).isEqualTo("claimant-a");
    }

    @Test
    void managerCannotVerifyPhoneClaims() {
        assertThatThrownBy(() -> service.verify(MANAGER, UUID.randomUUID(), new VerifyClaimCommand("POL-1", "123456")))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(claims, otpVerifier);
    }
}

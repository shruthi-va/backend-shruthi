package com.chubb.claims.claim.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chubb.claims.claim.application.event.PolicyChangedPayload;
import com.chubb.claims.claim.application.port.out.PolicyProjectionRepository;
import com.chubb.claims.claim.application.port.out.ProcessedEventRepository;
import com.chubb.claims.claim.domain.model.PolicyProjection;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyProjectionServiceTest {

    private static final UUID POLICY_ID = UUID.randomUUID();

    @Mock
    private PolicyProjectionRepository projections;

    @Mock
    private ProcessedEventRepository processed;

    private PolicyProjectionService service;

    @BeforeEach
    void setUp() {
        service = new PolicyProjectionService(projections, processed);
    }

    @Test
    void duplicateEventIsSkipped() {
        UUID eventId = UUID.randomUUID();
        when(processed.exists(eventId)).thenReturn(true);

        service.apply(eventId, payload(2L));

        verifyNoInteractions(projections);
        verify(processed, never()).markProcessed(any());
    }

    @Test
    void newerRevisionReplacesProjection() {
        UUID eventId = UUID.randomUUID();
        when(projections.findByPolicyId(POLICY_ID)).thenReturn(Optional.of(projection(1L)));

        service.apply(eventId, payload(2L));

        verify(projections).save(argThat(saved -> saved.version() == 2L));
        verify(processed).markProcessed(eventId);
    }

    @Test
    void olderRevisionIsIgnoredButMarkedProcessed() {
        UUID eventId = UUID.randomUUID();
        when(projections.findByPolicyId(POLICY_ID)).thenReturn(Optional.of(projection(3L)));

        service.apply(eventId, payload(2L));

        verify(projections, never()).save(any());
        verify(processed).markProcessed(eventId);
    }

    @Test
    void unknownPolicyIsCreatedFromEvent() {
        UUID eventId = UUID.randomUUID();
        when(projections.findByPolicyId(POLICY_ID)).thenReturn(Optional.empty());

        service.apply(eventId, payload(1L));

        verify(projections).save(argThat(saved -> saved.policyId().equals(POLICY_ID)
                && saved.market().equals("SG")
                && saved.version() == 1L));
        verify(processed).markProcessed(eventId);
    }

    private static PolicyChangedPayload payload(long revision) {
        return new PolicyChangedPayload(POLICY_ID, "POL-1", "SG", "MOTOR", "SGD",
                "claimant-a", "ACTIVE", revision);
    }

    private static PolicyProjection projection(long version) {
        return new PolicyProjection(POLICY_ID, "POL-1", "SG", "SGD", "claimant-a", "ACTIVE", version);
    }
}

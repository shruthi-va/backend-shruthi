package com.chubb.claims.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chubb.claims.reporting.application.event.ClaimStatusChangedPayload;
import com.chubb.claims.reporting.application.port.out.ClaimSnapshotRepository;
import com.chubb.claims.reporting.application.port.out.ProcessedEventRepository;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimStatusIngestServiceTest {

    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final Instant EARLIER = Instant.parse("2024-05-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2024-05-01T11:00:00Z");

    @Mock
    private ClaimSnapshotRepository snapshots;

    @Mock
    private ProcessedEventRepository processed;

    private ClaimStatusIngestService service;

    @BeforeEach
    void setUp() {
        service = new ClaimStatusIngestService(snapshots, processed);
    }

    @Test
    void duplicateEventIsSkipped() {
        UUID eventId = UUID.randomUUID();
        when(processed.exists(eventId)).thenReturn(true);

        service.apply(eventId, LATER, payload("UNDER_REVIEW"));

        verifyNoInteractions(snapshots);
        verify(processed, never()).markProcessed(any());
    }

    @Test
    void newClaimIsSavedWithEventTimeAsUpdatedAt() {
        UUID eventId = UUID.randomUUID();
        when(processed.exists(eventId)).thenReturn(false);
        when(snapshots.findById(CLAIM_ID)).thenReturn(Optional.empty());

        service.apply(eventId, LATER, payload("SUBMITTED"));

        verify(snapshots).save(argThat(saved -> saved.status() == ClaimStatus.SUBMITTED
                && saved.updatedAt().equals(LATER)
                && saved.claimedAmount().compareTo(new BigDecimal("1500.00")) == 0));
        verify(processed).markProcessed(eventId);
    }

    @Test
    void olderEventDoesNotOverwriteNewerSnapshot() {
        UUID eventId = UUID.randomUUID();
        when(processed.exists(eventId)).thenReturn(false);
        when(snapshots.findById(CLAIM_ID)).thenReturn(Optional.of(snapshot(LATER)));

        service.apply(eventId, EARLIER, payload("SUBMITTED"));

        verify(snapshots, never()).save(any());
        verify(processed).markProcessed(eventId);
    }

    @Test
    void eventWithSameTimestampIsApplied() {
        UUID eventId = UUID.randomUUID();
        when(processed.exists(eventId)).thenReturn(false);
        when(snapshots.findById(CLAIM_ID)).thenReturn(Optional.of(snapshot(LATER)));

        service.apply(eventId, LATER, payload("UNDER_REVIEW"));

        verify(snapshots).save(argThat(saved -> saved.status() == ClaimStatus.UNDER_REVIEW));
    }

    private static ClaimStatusChangedPayload payload(String toStatus) {
        return new ClaimStatusChangedPayload(CLAIM_ID, "CLM-1", "NZ", toStatus, "MOTOR",
                new BigDecimal("1500.00"), "NZD", false);
    }

    private static ClaimSnapshot snapshot(Instant updatedAt) {
        return new ClaimSnapshot(CLAIM_ID, "CLM-1", "NZ", ClaimStatus.SUBMITTED, "MOTOR",
                new BigDecimal("1500.00"), "NZD", false, updatedAt);
    }
}

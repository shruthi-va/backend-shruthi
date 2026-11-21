package com.chubb.claims.reporting.application.service;

import com.chubb.claims.reporting.application.event.ClaimStatusChangedPayload;
import com.chubb.claims.reporting.application.port.out.ClaimSnapshotRepository;
import com.chubb.claims.reporting.application.port.out.ProcessedEventRepository;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies claims.status-changed to the snapshot. Duplicate events are skipped, and an event older than
 * the snapshot never overwrites it.
 */
@Service
public class ClaimStatusIngestService {

    private final ClaimSnapshotRepository snapshots;
    private final ProcessedEventRepository processed;

    public ClaimStatusIngestService(ClaimSnapshotRepository snapshots, ProcessedEventRepository processed) {
        this.snapshots = snapshots;
        this.processed = processed;
    }

    @Transactional
    public void apply(UUID eventId, Instant occurredAt, ClaimStatusChangedPayload payload) {
        if (processed.exists(eventId)) {
            return;
        }
        boolean newer = snapshots.findById(payload.claimId())
                .map(current -> !occurredAt.isBefore(current.updatedAt()))
                .orElse(true);
        if (newer) {
            snapshots.save(new ClaimSnapshot(
                    payload.claimId(),
                    payload.claimNumber(),
                    payload.market(),
                    ClaimStatus.valueOf(payload.toStatus()),
                    payload.claimType(),
                    payload.claimedAmount(),
                    payload.currency(),
                    payload.auditFlag(),
                    occurredAt));
        }
        processed.markProcessed(eventId);
    }
}

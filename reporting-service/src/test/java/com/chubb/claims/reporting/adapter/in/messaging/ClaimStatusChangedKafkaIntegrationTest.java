package com.chubb.claims.reporting.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.chubb.claims.common.event.DomainEvent;
import com.chubb.claims.reporting.application.event.ClaimStatusChangedPayload;
import com.chubb.claims.reporting.application.event.ClaimTopics;
import com.chubb.claims.reporting.application.port.out.ClaimSnapshotRepository;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import com.chubb.claims.reporting.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

class ClaimStatusChangedKafkaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClaimSnapshotRepository snapshots;

    @Test
    void statusChangedEventFromKafkaCreatesSnapshot() throws Exception {
        UUID claimId = UUID.randomUUID();
        var payload = new ClaimStatusChangedPayload(claimId, "CLM-KAFKA", "KR", "UNDER_REVIEW", "PROPERTY",
                new BigDecimal("4200.00"), "KRW", true);
        var event = new DomainEvent<>(UUID.randomUUID(), "ClaimStatusChanged", claimId.toString(), "KR",
                Instant.now(), payload);

        kafka.send(ClaimTopics.STATUS_CHANGED, claimId.toString(), objectMapper.writeValueAsString(event)).get();

        ClaimSnapshot snapshot = awaitSnapshot(claimId);
        assertThat(snapshot.status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(snapshot.market()).isEqualTo("KR");
        assertThat(snapshot.auditFlag()).isTrue();
    }

    private ClaimSnapshot awaitSnapshot(UUID claimId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Optional<ClaimSnapshot> found = snapshots.findById(claimId);
            if (found.isPresent()) {
                return found.get();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Claim " + claimId + " was not snapshotted within 10 seconds");
    }
}

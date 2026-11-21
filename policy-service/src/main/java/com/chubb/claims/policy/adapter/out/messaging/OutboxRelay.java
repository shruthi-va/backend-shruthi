package com.chubb.claims.policy.adapter.out.messaging;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes unsent outbox rows in creation order. A row is marked published only after Kafka acknowledges it,
 * so delivery is at least once and consumers must de-duplicate by eventId.
 */
@Component
class OutboxRelay {

    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    OutboxRelay(OutboxJpaRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${policy.outbox.relay-interval-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEntity> pending = outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEntity event : pending) {
            try {
                kafka.send(event.getTopic(), event.getMessageKey(), event.getPayload()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                return;
            }
            event.markPublished(Instant.now());
        }
    }
}

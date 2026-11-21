package com.chubb.claims.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope for all Kafka messages. The aggregateId is used as the Kafka record key,
 * so events for the same claim stay in order within a partition.
 *
 * @param eventId     unique ID, used by consumers to detect duplicates
 * @param eventType   e.g. ClaimSubmitted, ReserveChanged, ClaimSettled
 * @param aggregateId claim or reserve ID
 * @param market      market code, e.g. SG, HK, IN
 * @param occurredAt  when the change happened
 * @param payload     event body (serialized to JSON)
 */
public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        String aggregateId,
        String market,
        Instant occurredAt,
        T payload) {

    public static <T> DomainEvent<T> of(String eventType, String aggregateId, String market, T payload) {
        return new DomainEvent<>(UUID.randomUUID(), eventType, aggregateId, market, Instant.now(), payload);
    }
}

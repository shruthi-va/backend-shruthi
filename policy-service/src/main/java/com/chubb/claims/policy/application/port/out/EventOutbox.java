package com.chubb.claims.policy.application.port.out;

import com.chubb.claims.common.event.DomainEvent;

/**
 * Stores an event in the same transaction as the business change. A relay publishes it to Kafka afterwards.
 */
public interface EventOutbox {

    void append(String topic, String key, DomainEvent<?> event);
}

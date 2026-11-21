package com.chubb.claims.policy.adapter.out.messaging;

import com.chubb.claims.common.event.DomainEvent;
import com.chubb.claims.policy.application.port.out.EventOutbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaEventOutbox implements EventOutbox {

    private final OutboxJpaRepository jpa;
    private final ObjectMapper objectMapper;

    JpaEventOutbox(OutboxJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(String topic, String key, DomainEvent<?> event) {
        try {
            jpa.save(new OutboxEntity(UUID.randomUUID(), topic, key,
                    objectMapper.writeValueAsString(event), Instant.now()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + event.eventType(), e);
        }
    }
}

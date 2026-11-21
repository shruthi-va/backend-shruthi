package com.chubb.claims.claim.adapter.out.persistence;

import com.chubb.claims.claim.application.port.out.ProcessedEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
class ProcessedEventPersistenceAdapter implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository jpa;

    ProcessedEventPersistenceAdapter(ProcessedEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(UUID eventId) {
        return jpa.existsById(eventId);
    }

    @Override
    public void markProcessed(UUID eventId) {
        jpa.save(new ProcessedEventEntity(eventId, Instant.now()));
    }
}

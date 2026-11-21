package com.chubb.claims.reporting.adapter.out.persistence;

import com.chubb.claims.reporting.application.port.out.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

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

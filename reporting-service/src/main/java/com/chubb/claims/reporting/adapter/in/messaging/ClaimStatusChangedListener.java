package com.chubb.claims.reporting.adapter.in.messaging;

import com.chubb.claims.common.event.DomainEvent;
import com.chubb.claims.reporting.application.event.ClaimStatusChangedPayload;
import com.chubb.claims.reporting.application.event.ClaimTopics;
import com.chubb.claims.reporting.application.service.ClaimStatusIngestService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ClaimStatusChangedListener {

    private static final TypeReference<DomainEvent<ClaimStatusChangedPayload>> EVENT_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final ClaimStatusIngestService ingest;

    public ClaimStatusChangedListener(ObjectMapper objectMapper, ClaimStatusIngestService ingest) {
        this.objectMapper = objectMapper;
        this.ingest = ingest;
    }

    @KafkaListener(topics = ClaimTopics.STATUS_CHANGED)
    public void onStatusChanged(String message) throws Exception {
        DomainEvent<ClaimStatusChangedPayload> event = objectMapper.readValue(message, EVENT_TYPE);
        ingest.apply(event.eventId(), event.occurredAt(), event.payload());
    }
}

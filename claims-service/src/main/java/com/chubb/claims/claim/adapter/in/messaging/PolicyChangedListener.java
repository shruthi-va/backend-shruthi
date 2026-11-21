package com.chubb.claims.claim.adapter.in.messaging;

import com.chubb.claims.claim.application.event.ClaimTopics;
import com.chubb.claims.claim.application.event.PolicyChangedPayload;
import com.chubb.claims.claim.application.service.PolicyProjectionService;
import com.chubb.claims.common.event.DomainEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PolicyChangedListener {

    private static final TypeReference<DomainEvent<PolicyChangedPayload>> EVENT_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final PolicyProjectionService projections;

    public PolicyChangedListener(ObjectMapper objectMapper, PolicyProjectionService projections) {
        this.objectMapper = objectMapper;
        this.projections = projections;
    }

    @KafkaListener(topics = ClaimTopics.POLICY_CHANGED)
    public void onPolicyChanged(String message) throws Exception {
        DomainEvent<PolicyChangedPayload> event = objectMapper.readValue(message, EVENT_TYPE);
        projections.apply(event.eventId(), event.payload());
    }
}

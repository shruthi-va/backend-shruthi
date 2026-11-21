package com.chubb.claims.claim.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.chubb.claims.claim.application.event.ClaimTopics;
import com.chubb.claims.claim.application.event.PolicyChangedPayload;
import com.chubb.claims.claim.application.port.out.PolicyProjectionRepository;
import com.chubb.claims.claim.domain.model.PolicyProjection;
import com.chubb.claims.claim.support.AbstractIntegrationTest;
import com.chubb.claims.common.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

class PolicyChangedKafkaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PolicyProjectionRepository projections;

    @Test
    void policyChangedEventFromKafkaIsProjectedLocally() throws Exception {
        UUID policyId = UUID.randomUUID();
        String owner = "claimant-" + policyId.toString().substring(0, 8);
        var payload = new PolicyChangedPayload(policyId, "POL-KAFKA", IN_MARKET, "MOTOR", INR, owner, "ACTIVE", 1L);
        var event = new DomainEvent<>(UUID.randomUUID(), "PolicyChanged", policyId.toString(), IN_MARKET,
                Instant.now(), payload);

        kafka.send(ClaimTopics.POLICY_CHANGED, policyId.toString(), objectMapper.writeValueAsString(event)).get();

        PolicyProjection projection = awaitProjection(policyId);
        assertThat(projection.ownerSubject()).isEqualTo(owner);
        assertThat(projection.isActive()).isTrue();
    }

    private PolicyProjection awaitProjection(UUID policyId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Optional<PolicyProjection> found = projections.findByPolicyId(policyId);
            if (found.isPresent()) {
                return found.get();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Policy " + policyId + " was not projected within 10 seconds");
    }
}

package com.chubb.claims.policy.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chubb.claims.policy.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class PolicyOutboxKafkaIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPIC = "policies.changed";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void createdPolicyIsRelayedFromOutboxToPoliciesChanged() throws Exception {
        String number = newPolicyNumber();
        MvcResult result = mvc.perform(post("/api/policies").with(as("mgr-kafka", IN_MARKET, "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson(number, IN_MARKET, INR)))
                .andExpect(status().isCreated())
                .andReturn();
        String policyId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        Map<String, Object> props = KafkaTestUtils.consumerProps("policy-outbox-" + UUID.randomUUID(), "false", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecord<String, String> record = awaitRecord(consumer, policyId, Duration.ofSeconds(10));

            assertThat(record.value()).contains(number);
        }
    }

    /** The topic is shared across tests, so skip records for other policies until the expected key arrives. */
    private static ConsumerRecord<String, String> awaitRecord(Consumer<String, String> consumer, String key,
                                                              Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                if (key.equals(record.key())) {
                    return record;
                }
            }
        }
        throw new AssertionError("No " + TOPIC + " record for policy " + key);
    }
}

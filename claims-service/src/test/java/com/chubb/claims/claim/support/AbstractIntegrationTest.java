package com.chubb.claims.claim.support;

import com.chubb.claims.claim.application.event.ClaimTopics;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.kafka.test.context.EmbeddedKafka;

/**
 * Full application context with H2, Flyway and an embedded Kafka broker.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = ClaimTopics.POLICY_CHANGED)
@Import(TestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    protected static final String IN_MARKET = "IN";
    protected static final String INR = "INR";

    /** A caller with the given Keycloak role and market claim, as the resource server would see it. */
    protected static RequestPostProcessor as(String subject, String market, String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(subject).claim("market", market))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}

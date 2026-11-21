package com.chubb.claims.reporting.support;

import com.chubb.claims.reporting.application.event.ClaimTopics;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Full application context with H2, Flyway and an embedded Kafka broker.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = ClaimTopics.STATUS_CHANGED)
@Import(TestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    /** A caller with the given Keycloak role and market claim, as the resource server would see it. */
    protected static RequestPostProcessor as(String subject, String market, String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(subject).claim("market", market))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}

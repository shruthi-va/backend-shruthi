package com.chubb.claims.policy.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Replaces the issuer-based decoder so the context starts without Keycloak.
 * Tests authenticate with SecurityMockMvcRequestPostProcessors.jwt(), so this decoder is never called.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            throw new JwtException("Bearer tokens are not accepted in tests");
        };
    }
}

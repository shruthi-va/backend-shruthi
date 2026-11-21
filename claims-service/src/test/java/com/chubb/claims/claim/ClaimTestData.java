package com.chubb.claims.claim;

import com.chubb.claims.claim.config.ClaimsProperties;
import com.chubb.claims.claim.domain.model.Channel;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimType;
import com.chubb.claims.claim.domain.model.PolicyProjection;
import com.chubb.claims.common.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared fixtures for claims-service unit tests.
 */
public final class ClaimTestData {

    public static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final LocalDate LOSS_DATE = LocalDate.of(2025, 6, 10);
    public static final ClaimsProperties PROPERTIES = new ClaimsProperties(
            30, Map.of("SGD", new BigDecimal("1000000")), new ClaimsProperties.Otp("123456"));

    public static final CurrentUser MANAGER = new CurrentUser("mgr-1", "SG", Set.of(CurrentUser.MANAGER));
    public static final CurrentUser SG_STAFF = new CurrentUser("staff-sg", "SG", Set.of(CurrentUser.CLAIMS_STAFF));
    public static final CurrentUser HK_STAFF = new CurrentUser("staff-hk", "HK", Set.of(CurrentUser.CLAIMS_STAFF));
    public static final CurrentUser CLAIMANT_A = new CurrentUser("claimant-a", "SG", Set.of(CurrentUser.CLAIMANT));
    public static final CurrentUser CLAIMANT_B = new CurrentUser("claimant-b", "SG", Set.of(CurrentUser.CLAIMANT));

    private ClaimTestData() {
    }

    public static PolicyProjection policy(String market, String currency, String ownerSubject) {
        return new PolicyProjection(UUID.randomUUID(), "POL-1", market, currency, ownerSubject, "ACTIVE", 1L);
    }

    public static Claim onlineClaim(PolicyProjection policy, BigDecimal amount) {
        return Claim.file(UUID.randomUUID(), "CLM-TEST0001", Channel.ONLINE, policy, policy.ownerSubject(), null,
                ClaimType.MOTOR, LOSS_DATE, amount, false, NOW);
    }

    public static Claim staffRecordedClaim(PolicyProjection policy, BigDecimal amount) {
        return Claim.file(UUID.randomUUID(), "CLM-TEST0002", Channel.STAFF_RECORDED, policy, null, UUID.randomUUID(),
                ClaimType.MOTOR, LOSS_DATE, amount, false, NOW);
    }
}

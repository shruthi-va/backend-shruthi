package com.chubb.claims.claim.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ReserveChangedPayload(
        UUID claimId,
        String claimNumber,
        String market,
        String currency,
        BigDecimal proposedReserve,
        BigDecimal approvedReserve,
        String action) {
}

package com.chubb.claims.claim.domain.rules;

import com.chubb.claims.common.error.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public final class ClaimRules {

    private ClaimRules() {
    }

    /**
     * BR-1: the claim must be filed within the window after the date of loss, and the loss cannot be in the future.
     */
    public static void requireFilingWindow(LocalDate dateOfLoss, LocalDate filedOn, int windowDays) {
        if (dateOfLoss.isAfter(filedOn)) {
            throw new BusinessRuleException("Date of loss cannot be in the future");
        }
        if (filedOn.isAfter(dateOfLoss.plusDays(windowDays))) {
            throw new BusinessRuleException("Claim is outside the " + windowDays + "-day filing window");
        }
    }

    /**
     * BR-4: audit flag is set when the amount reaches the threshold for its currency.
     * A missing threshold sets the flag, so an unconfigured market is reviewed rather than missed.
     */
    public static boolean requiresAudit(BigDecimal claimedAmount, Optional<BigDecimal> threshold) {
        return threshold.map(limit -> claimedAmount.compareTo(limit) >= 0).orElse(true);
    }
}

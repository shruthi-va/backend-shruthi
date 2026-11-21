package com.chubb.claims.claim.adapter.in.web.dto;

import com.chubb.claims.claim.domain.model.ClaimType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FileClaimRequest(
        @NotNull UUID policyId,
        @NotNull ClaimType claimType,
        @NotNull LocalDate dateOfLoss,
        @NotNull @DecimalMin(value = "0.01") BigDecimal claimedAmount) {
}

package com.chubb.claims.policy.adapter.in.web.dto;

import com.chubb.claims.policy.domain.model.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreatePolicyRequest(
        @NotBlank String policyNumber,
        @NotBlank String market,
        @NotNull ProductType productType,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {
}

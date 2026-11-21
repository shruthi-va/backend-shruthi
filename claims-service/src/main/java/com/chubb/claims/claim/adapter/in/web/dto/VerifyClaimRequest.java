package com.chubb.claims.claim.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyClaimRequest(@NotBlank String policyNumber, @NotBlank String otp) {
}

package com.chubb.claims.policy.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OwnContactRequest(
        @NotBlank String fullName,
        @Email String email,
        @Pattern(regexp = "\\+?[0-9 ()-]{6,32}") String phone,
        boolean emailOptIn,
        boolean smsOptIn) {
}

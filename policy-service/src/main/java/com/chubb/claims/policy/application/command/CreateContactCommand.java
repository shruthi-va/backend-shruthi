package com.chubb.claims.policy.application.command;

public record CreateContactCommand(
        String market,
        String fullName,
        String email,
        String phone,
        boolean emailOptIn,
        boolean smsOptIn) {
}

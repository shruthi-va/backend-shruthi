package com.chubb.claims.claim.application.command;

public record VerifyClaimCommand(String policyNumber, String otp) {
}

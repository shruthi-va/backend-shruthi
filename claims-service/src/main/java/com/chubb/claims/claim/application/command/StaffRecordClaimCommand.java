package com.chubb.claims.claim.application.command;

import com.chubb.claims.claim.domain.model.ClaimType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StaffRecordClaimCommand(UUID policyId, UUID contactId, ClaimType claimType, LocalDate dateOfLoss,
                                      BigDecimal claimedAmount) {
}

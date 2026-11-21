package com.chubb.claims.policy.application.command;

import com.chubb.claims.policy.domain.model.ProductType;

public record CreatePolicyCommand(String policyNumber, String market, ProductType productType, String currency) {
}

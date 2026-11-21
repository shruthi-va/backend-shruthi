package com.chubb.claims.claim.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record DecisionRequest(@NotNull Boolean approve) {
}

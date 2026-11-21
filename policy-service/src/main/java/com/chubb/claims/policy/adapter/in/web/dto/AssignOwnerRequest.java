package com.chubb.claims.policy.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignOwnerRequest(@NotBlank String ownerSubject) {
}

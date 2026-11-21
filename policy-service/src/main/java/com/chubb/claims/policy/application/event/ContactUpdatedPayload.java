package com.chubb.claims.policy.application.event;

/** Payload of claimants.contact-updated. Carries no personal data (see PRD section 14). */
public record ContactUpdatedPayload(String ownerSubject, boolean emailOptIn, boolean smsOptIn) {
}

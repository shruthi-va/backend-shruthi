package com.chubb.claims.policy.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Contact details for a claimant. A contact recorded by staff for a phone claim has no owner
 * until the claimant verifies it; a self-registered contact is owned by the claimant's subject.
 */
public class ClaimantContact {

    private final UUID id;
    private final UUID contactId;
    private String ownerSubject;
    private final String market;
    private String fullName;
    private String email;
    private String phone;
    private boolean emailOptIn;
    private boolean smsOptIn;
    private long revision;
    private Instant updatedAt;

    private ClaimantContact(UUID id, UUID contactId, String ownerSubject, String market, String fullName,
                            String email, String phone, boolean emailOptIn, boolean smsOptIn,
                            long revision, Instant updatedAt) {
        this.id = id;
        this.contactId = contactId;
        this.ownerSubject = ownerSubject;
        this.market = market;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.emailOptIn = emailOptIn;
        this.smsOptIn = smsOptIn;
        this.revision = revision;
        this.updatedAt = updatedAt;
    }

    public static ClaimantContact recordedByStaff(String market, String fullName, String email, String phone,
                                                  boolean emailOptIn, boolean smsOptIn) {
        return new ClaimantContact(UUID.randomUUID(), UUID.randomUUID(), null, market, fullName,
                email, phone, emailOptIn, smsOptIn, 1L, Instant.now());
    }

    public static ClaimantContact forOwner(String ownerSubject, String market, String fullName, String email,
                                           String phone, boolean emailOptIn, boolean smsOptIn) {
        return new ClaimantContact(UUID.randomUUID(), UUID.randomUUID(), ownerSubject, market, fullName,
                email, phone, emailOptIn, smsOptIn, 1L, Instant.now());
    }

    public static ClaimantContact restore(UUID id, UUID contactId, String ownerSubject, String market,
                                          String fullName, String email, String phone, boolean emailOptIn,
                                          boolean smsOptIn, long revision, Instant updatedAt) {
        return new ClaimantContact(id, contactId, ownerSubject, market, fullName, email, phone,
                emailOptIn, smsOptIn, revision, updatedAt);
    }

    /**
     * Returns true when the notification preferences changed, so the caller knows whether to publish.
     */
    public boolean updateDetails(String fullName, String email, String phone, boolean emailOptIn, boolean smsOptIn) {
        boolean optInChanged = this.emailOptIn != emailOptIn || this.smsOptIn != smsOptIn;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.emailOptIn = emailOptIn;
        this.smsOptIn = smsOptIn;
        this.revision++;
        this.updatedAt = Instant.now();
        return optInChanged;
    }

    public UUID id() { return id; }
    public UUID contactId() { return contactId; }
    public String ownerSubject() { return ownerSubject; }
    public String market() { return market; }
    public String fullName() { return fullName; }
    public String email() { return email; }
    public String phone() { return phone; }
    public boolean emailOptIn() { return emailOptIn; }
    public boolean smsOptIn() { return smsOptIn; }
    public long revision() { return revision; }
    public Instant updatedAt() { return updatedAt; }
}

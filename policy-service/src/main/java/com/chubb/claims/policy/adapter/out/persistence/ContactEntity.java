package com.chubb.claims.policy.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claimant_contact")
class ContactEntity {

    @Id
    private UUID id;

    @Column(name = "contact_id", nullable = false, unique = true)
    private UUID contactId;

    @Column(name = "owner_subject", unique = true)
    private String ownerSubject;

    @Column(nullable = false)
    private String market;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    private String phone;

    @Column(name = "email_opt_in", nullable = false)
    private boolean emailOptIn;

    @Column(name = "sms_opt_in", nullable = false)
    private boolean smsOptIn;

    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContactEntity() {
    }

    ContactEntity(UUID id, UUID contactId, String ownerSubject, String market, String fullName, String email,
                  String phone, boolean emailOptIn, boolean smsOptIn, long version, Instant updatedAt) {
        this.id = id;
        this.contactId = contactId;
        this.ownerSubject = ownerSubject;
        this.market = market;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.emailOptIn = emailOptIn;
        this.smsOptIn = smsOptIn;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    UUID getId() { return id; }
    UUID getContactId() { return contactId; }
    String getOwnerSubject() { return ownerSubject; }
    String getMarket() { return market; }
    String getFullName() { return fullName; }
    String getEmail() { return email; }
    String getPhone() { return phone; }
    boolean isEmailOptIn() { return emailOptIn; }
    boolean isSmsOptIn() { return smsOptIn; }
    long getVersion() { return version; }
    Instant getUpdatedAt() { return updatedAt; }
}

package com.chubb.claims.policy.application.service;

import com.chubb.claims.common.error.BusinessRuleException;
import com.chubb.claims.common.event.DomainEvent;
import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.policy.application.command.CreateContactCommand;
import com.chubb.claims.policy.application.event.ContactUpdatedPayload;
import com.chubb.claims.policy.application.port.out.ContactRepository;
import com.chubb.claims.policy.application.port.out.EventOutbox;
import com.chubb.claims.policy.domain.model.ClaimantContact;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claimant contact details. Staff record contacts for phone claims; claimants maintain their own.
 */
@Service
@Transactional
public class ContactService {

    static final String TOPIC = "claimants.contact-updated";
    private static final String EVENT_TYPE = "ClaimantContactUpdated";

    private final ContactRepository contacts;
    private final EventOutbox outbox;

    public ContactService(ContactRepository contacts, EventOutbox outbox) {
        this.contacts = contacts;
        this.outbox = outbox;
    }

    public ClaimantContact recordByStaff(CurrentUser user, CreateContactCommand command) {
        if (!user.isStaff() && !user.isManager()) {
            throw new AccessDeniedException("Only claims staff or managers can record contacts");
        }
        if (user.isStaff() && !command.market().equals(user.market())) {
            throw new AccessDeniedException("Staff can only record contacts in their own market");
        }
        ClaimantContact contact = ClaimantContact.recordedByStaff(command.market(), command.fullName(),
                command.email(), command.phone(), command.emailOptIn(), command.smsOptIn());
        return contacts.save(contact);
    }

    /**
     * Creates or updates the calling claimant's contact. Publishes claimants.contact-updated
     * only when the notification preferences change or the contact is new.
     */
    public ClaimantContact upsertOwnContact(CurrentUser user, CreateContactCommand command) {
        if (!user.isClaimant()) {
            throw new AccessDeniedException("Only claimants can maintain their own contact");
        }
        if (user.market() == null) {
            throw new BusinessRuleException("Token has no market claim");
        }
        ClaimantContact contact;
        boolean publish;
        var existing = contacts.findByOwnerSubject(user.subject());
        if (existing.isPresent()) {
            contact = existing.get();
            publish = contact.updateDetails(command.fullName(), command.email(), command.phone(),
                    command.emailOptIn(), command.smsOptIn());
        } else {
            contact = ClaimantContact.forOwner(user.subject(), user.market(), command.fullName(),
                    command.email(), command.phone(), command.emailOptIn(), command.smsOptIn());
            publish = true;
        }
        contacts.save(contact);
        if (publish) {
            outbox.append(TOPIC, user.subject(), DomainEvent.of(EVENT_TYPE, user.subject(), contact.market(),
                    new ContactUpdatedPayload(user.subject(), contact.emailOptIn(), contact.smsOptIn())));
        }
        return contact;
    }
}

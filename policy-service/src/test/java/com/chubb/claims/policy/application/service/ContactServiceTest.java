package com.chubb.claims.policy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chubb.claims.common.error.BusinessRuleException;
import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.policy.application.command.CreateContactCommand;
import com.chubb.claims.policy.application.port.out.ContactRepository;
import com.chubb.claims.policy.application.port.out.EventOutbox;
import com.chubb.claims.policy.domain.model.ClaimantContact;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    private static final CurrentUser SG_STAFF = new CurrentUser("staff-sg", "SG", Set.of(CurrentUser.CLAIMS_STAFF));
    private static final CurrentUser CLAIMANT = new CurrentUser("claimant-a", "SG", Set.of(CurrentUser.CLAIMANT));
    private static final CurrentUser CLAIMANT_NO_MARKET = new CurrentUser("claimant-x", null, Set.of(CurrentUser.CLAIMANT));

    @Mock
    private ContactRepository contacts;

    @Mock
    private EventOutbox outbox;

    private ContactService service;

    @BeforeEach
    void setUp() {
        service = new ContactService(contacts, outbox);
    }

    @Test
    void staffRecordsContactInTheirOwnMarketWithoutOwner() {
        when(contacts.save(any(ClaimantContact.class))).thenAnswer(inv -> inv.getArgument(0));

        ClaimantContact contact = service.recordByStaff(SG_STAFF, command("SG", true, false));

        assertThat(contact.ownerSubject()).isNull();
        assertThat(contact.contactId()).isNotNull();
        assertThat(contact.market()).isEqualTo("SG");
    }

    @Test
    void staffCannotRecordContactInAnotherMarket() {
        assertThatThrownBy(() -> service.recordByStaff(SG_STAFF, command("HK", true, false)))
                .isInstanceOf(AccessDeniedException.class);

        verify(contacts, never()).save(any());
    }

    @Test
    void claimantCannotRecordContactForPhoneClaim() {
        assertThatThrownBy(() -> service.recordByStaff(CLAIMANT, command("SG", true, false)))
                .isInstanceOf(AccessDeniedException.class);

        verify(contacts, never()).save(any());
    }

    @Test
    void newOwnContactIsSavedAndPublished() {
        when(contacts.findByOwnerSubject("claimant-a")).thenReturn(Optional.empty());
        when(contacts.save(any(ClaimantContact.class))).thenAnswer(inv -> inv.getArgument(0));

        ClaimantContact contact = service.upsertOwnContact(CLAIMANT, command("SG", true, true));

        assertThat(contact.ownerSubject()).isEqualTo("claimant-a");
        verify(outbox).append(eq("claimants.contact-updated"), eq("claimant-a"),
                argThat(event -> "ClaimantContactUpdated".equals(event.eventType())));
    }

    @Test
    void updateWithUnchangedOptInsDoesNotPublish() {
        ClaimantContact existing = ClaimantContact.forOwner("claimant-a", "SG", "Old Name",
                "a@example.com", "+6590000000", true, false);
        when(contacts.findByOwnerSubject("claimant-a")).thenReturn(Optional.of(existing));
        when(contacts.save(existing)).thenReturn(existing);

        ClaimantContact updated = service.upsertOwnContact(CLAIMANT, command("SG", true, false));

        assertThat(updated.fullName()).isEqualTo("Test Claimant");
        verifyNoInteractions(outbox);
    }

    @Test
    void updateThatChangesOptInsPublishes() {
        ClaimantContact existing = ClaimantContact.forOwner("claimant-a", "SG", "Test Claimant",
                "a@example.com", "+6590000000", true, false);
        when(contacts.findByOwnerSubject("claimant-a")).thenReturn(Optional.of(existing));
        when(contacts.save(existing)).thenReturn(existing);

        service.upsertOwnContact(CLAIMANT, command("SG", true, true));

        verify(outbox).append(eq("claimants.contact-updated"), eq("claimant-a"),
                argThat(event -> "ClaimantContactUpdated".equals(event.eventType())));
    }

    @Test
    void staffCannotMaintainOwnContact() {
        assertThatThrownBy(() -> service.upsertOwnContact(SG_STAFF, command("SG", true, false)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(contacts, outbox);
    }

    @Test
    void claimantWithoutMarketClaimIsRejected() {
        assertThatThrownBy(() -> service.upsertOwnContact(CLAIMANT_NO_MARKET, command("SG", true, false)))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(contacts, outbox);
    }

    private static CreateContactCommand command(String market, boolean emailOptIn, boolean smsOptIn) {
        return new CreateContactCommand(market, "Test Claimant", "a@example.com", "+6590000000",
                emailOptIn, smsOptIn);
    }
}

package com.chubb.claims.policy.adapter.in.web;

import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.policy.adapter.in.web.dto.ContactResponse;
import com.chubb.claims.policy.adapter.in.web.dto.OwnContactRequest;
import com.chubb.claims.policy.adapter.in.web.dto.StaffContactRequest;
import com.chubb.claims.policy.application.command.CreateContactCommand;
import com.chubb.claims.policy.application.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Claimant contacts", description = "Contact details and notification preferences")
public class ContactController {

    private final ContactService contacts;

    public ContactController(ContactService contacts) {
        this.contacts = contacts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a contact for a phone claim (staff in own market, or manager)")
    public ContactResponse recordByStaff(@Valid @RequestBody StaffContactRequest request, Authentication auth) {
        var command = new CreateContactCommand(request.market(), request.fullName(), request.email(),
                request.phone(), request.emailOptIn(), request.smsOptIn());
        return ContactResponse.from(contacts.recordByStaff(CurrentUser.from(auth), command));
    }

    @PutMapping("/me")
    @Operation(summary = "Create or update the calling claimant's contact details")
    public ContactResponse upsertOwn(@Valid @RequestBody OwnContactRequest request, Authentication auth) {
        var command = new CreateContactCommand(null, request.fullName(), request.email(),
                request.phone(), request.emailOptIn(), request.smsOptIn());
        return ContactResponse.from(contacts.upsertOwnContact(CurrentUser.from(auth), command));
    }
}

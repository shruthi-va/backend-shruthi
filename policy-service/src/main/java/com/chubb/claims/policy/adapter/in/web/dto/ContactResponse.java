package com.chubb.claims.policy.adapter.in.web.dto;

import com.chubb.claims.policy.domain.model.ClaimantContact;
import java.util.UUID;

public record ContactResponse(
        UUID contactId,
        String market,
        String fullName,
        String email,
        String phone,
        boolean emailOptIn,
        boolean smsOptIn) {

    public static ContactResponse from(ClaimantContact c) {
        return new ContactResponse(c.contactId(), c.market(), c.fullName(), c.email(), c.phone(),
                c.emailOptIn(), c.smsOptIn());
    }
}

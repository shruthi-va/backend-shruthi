package com.chubb.claims.policy.application.port.out;

import com.chubb.claims.policy.domain.model.ClaimantContact;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepository {

    ClaimantContact save(ClaimantContact contact);

    Optional<ClaimantContact> findByOwnerSubject(String ownerSubject);
}

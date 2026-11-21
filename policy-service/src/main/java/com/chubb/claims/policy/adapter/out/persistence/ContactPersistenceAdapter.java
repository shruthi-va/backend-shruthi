package com.chubb.claims.policy.adapter.out.persistence;

import com.chubb.claims.policy.application.port.out.ContactRepository;
import com.chubb.claims.policy.domain.model.ClaimantContact;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ContactPersistenceAdapter implements ContactRepository {

    private final ContactJpaRepository jpa;

    ContactPersistenceAdapter(ContactJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ClaimantContact save(ClaimantContact c) {
        jpa.save(new ContactEntity(c.id(), c.contactId(), c.ownerSubject(), c.market(), c.fullName(), c.email(),
                c.phone(), c.emailOptIn(), c.smsOptIn(), c.revision(), c.updatedAt()));
        return c;
    }

    @Override
    public Optional<ClaimantContact> findByOwnerSubject(String ownerSubject) {
        return jpa.findByOwnerSubject(ownerSubject).map(e -> ClaimantContact.restore(e.getId(), e.getContactId(),
                e.getOwnerSubject(), e.getMarket(), e.getFullName(), e.getEmail(), e.getPhone(),
                e.isEmailOptIn(), e.isSmsOptIn(), e.getVersion(), e.getUpdatedAt()));
    }
}

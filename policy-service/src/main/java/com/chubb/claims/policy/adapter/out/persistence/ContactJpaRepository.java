package com.chubb.claims.policy.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContactJpaRepository extends JpaRepository<ContactEntity, UUID> {

    Optional<ContactEntity> findByOwnerSubject(String ownerSubject);
}

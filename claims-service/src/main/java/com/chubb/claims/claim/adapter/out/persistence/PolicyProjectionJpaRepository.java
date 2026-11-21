package com.chubb.claims.claim.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PolicyProjectionJpaRepository extends JpaRepository<PolicyProjectionEntity, UUID> {
}

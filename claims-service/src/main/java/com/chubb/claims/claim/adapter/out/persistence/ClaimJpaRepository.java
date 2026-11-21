package com.chubb.claims.claim.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface ClaimJpaRepository extends JpaRepository<ClaimEntity, UUID>, JpaSpecificationExecutor<ClaimEntity> {
}

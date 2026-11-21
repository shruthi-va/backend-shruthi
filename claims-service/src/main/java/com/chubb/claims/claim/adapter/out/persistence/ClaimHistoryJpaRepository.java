package com.chubb.claims.claim.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ClaimHistoryJpaRepository extends JpaRepository<ClaimHistoryEntity, Long> {

    List<ClaimHistoryEntity> findByClaimIdOrderByChangedAtAscIdAsc(UUID claimId);
}

package com.chubb.claims.reporting.application.port.out;

import java.util.Optional;
import java.util.UUID;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;

public interface ClaimSnapshotRepository {

    Optional<ClaimSnapshot> findById(UUID claimId);

    void save(ClaimSnapshot snapshot);
}

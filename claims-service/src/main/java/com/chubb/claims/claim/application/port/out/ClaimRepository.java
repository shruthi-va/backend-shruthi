package com.chubb.claims.claim.application.port.out;

import com.chubb.claims.claim.application.query.ClaimFilter;
import com.chubb.claims.claim.application.query.PageResult;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.StatusChange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository {

    Claim save(Claim claim);

    Optional<Claim> findById(UUID id);

    PageResult<Claim> search(ClaimFilter filter);

    void appendHistory(StatusChange change);

    List<StatusChange> findHistory(UUID claimId);
}

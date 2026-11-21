package com.chubb.claims.reporting.adapter.out.persistence;

import com.chubb.claims.reporting.application.port.out.ClaimSnapshotRepository;
import com.chubb.claims.reporting.application.port.out.OutstandingClaimsReader;
import com.chubb.claims.reporting.application.query.OutstandingSummaryRow;
import com.chubb.claims.reporting.application.query.PageResult;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
class ClaimSnapshotPersistenceAdapter implements ClaimSnapshotRepository, OutstandingClaimsReader {

    private final ClaimSnapshotJpaRepository jpa;

    ClaimSnapshotPersistenceAdapter(ClaimSnapshotJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ClaimSnapshot> findById(UUID claimId) {
        return jpa.findById(claimId).map(ClaimSnapshotEntity::toDomain);
    }

    @Override
    public void save(ClaimSnapshot snapshot) {
        jpa.save(new ClaimSnapshotEntity(snapshot));
    }

    @Override
    public List<OutstandingSummaryRow> summarize(Set<ClaimStatus> statuses, String market) {
        return jpa.summarize(statuses, market);
    }

    @Override
    public PageResult<ClaimSnapshot> list(Set<ClaimStatus> statuses, String market, String claimType,
                                          int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ClaimSnapshotEntity> result = jpa.findOutstanding(statuses, market, claimType, pageable);
        return new PageResult<>(
                result.map(ClaimSnapshotEntity::toDomain).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}

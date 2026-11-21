package com.chubb.claims.claim.adapter.out.persistence;

import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.application.query.ClaimFilter;
import com.chubb.claims.claim.application.query.PageResult;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.StatusChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ClaimPersistenceAdapter implements ClaimRepository {

    private final ClaimJpaRepository claims;
    private final ClaimHistoryJpaRepository history;

    ClaimPersistenceAdapter(ClaimJpaRepository claims, ClaimHistoryJpaRepository history) {
        this.claims = claims;
        this.history = history;
    }

    @Override
    public Claim save(Claim claim) {
        ClaimEntity entity = claims.findById(claim.getId())
                .map(existing -> {
                    existing.applyChanges(claim);
                    return existing;
                })
                .orElseGet(() -> ClaimEntity.from(claim));
        return claims.save(entity).toDomain();
    }

    @Override
    public Optional<Claim> findById(UUID id) {
        return claims.findById(id).map(ClaimEntity::toDomain);
    }

    @Override
    public PageResult<Claim> search(ClaimFilter f) {
        Specification<ClaimEntity> spec = (root, query, cb) -> cb.conjunction();
        if (f.ownerSubject() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerSubject"), f.ownerSubject()));
        }
        if (f.market() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("market"), f.market()));
        }
        if (f.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), f.status()));
        }
        if (f.claimType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("claimType"), f.claimType()));
        }
        if (f.filedFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("filedAt"), f.filedFrom()));
        }
        if (f.filedTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<Instant>get("filedAt"), f.filedTo()));
        }
        Page<ClaimEntity> page = claims.findAll(spec,
                PageRequest.of(f.page(), f.size(), Sort.by(Sort.Direction.DESC, "filedAt")));
        List<Claim> items = page.getContent().stream().map(ClaimEntity::toDomain).toList();
        return new PageResult<>(items, f.page(), f.size(), page.getTotalElements());
    }

    @Override
    public void appendHistory(StatusChange change) {
        history.save(new ClaimHistoryEntity(change));
    }

    @Override
    public List<StatusChange> findHistory(UUID claimId) {
        return history.findByClaimIdOrderByChangedAtAscIdAsc(claimId).stream()
                .map(ClaimHistoryEntity::toDomain)
                .toList();
    }
}

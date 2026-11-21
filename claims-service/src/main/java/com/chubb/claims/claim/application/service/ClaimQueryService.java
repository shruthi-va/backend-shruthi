package com.chubb.claims.claim.application.service;

import com.chubb.claims.claim.application.query.ClaimFilter;
import com.chubb.claims.claim.application.query.PageResult;
import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.StatusChange;
import com.chubb.claims.common.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClaimQueryService {

    static final int MAX_PAGE_SIZE = 100;

    private final ClaimRepository claims;
    private final ClaimAccess access;

    public ClaimQueryService(ClaimRepository claims, ClaimAccess access) {
        this.claims = claims;
        this.access = access;
    }

    /**
     * Claimants see only their own claims. Staff are limited to their market. Managers can filter by any market.
     */
    @Transactional(readOnly = true)
    public PageResult<Claim> list(CurrentUser user, ClaimFilter requested) {
        int size = Math.min(Math.max(requested.size(), 1), MAX_PAGE_SIZE);
        ClaimFilter scoped = new ClaimFilter(
                user.isClaimant() ? user.subject() : null,
                user.isStaff() ? user.market() : requested.market(),
                requested.status(),
                requested.claimType(),
                requested.filedFrom(),
                requested.filedTo(),
                Math.max(requested.page(), 0),
                size);
        return claims.search(scoped);
    }

    @Transactional(readOnly = true)
    public Claim get(CurrentUser user, UUID claimId) {
        return access.loadVisible(user, claimId);
    }

    @Transactional(readOnly = true)
    public List<StatusChange> history(CurrentUser user, UUID claimId) {
        access.loadVisible(user, claimId);
        return claims.findHistory(claimId);
    }
}

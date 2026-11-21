package com.chubb.claims.claim.application.service;

import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.common.error.ResourceNotFoundException;
import com.chubb.claims.common.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Role and visibility rules shared by claim services. A claim the caller cannot see returns 404, so IDs can't be probed.
 */
@Component
class ClaimAccess {

    private final ClaimRepository claims;

    ClaimAccess(ClaimRepository claims) {
        this.claims = claims;
    }

    Claim loadVisible(CurrentUser user, UUID claimId) {
        Claim claim = claims.findById(claimId).orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));
        if (!canView(user, claim)) {
            throw new ResourceNotFoundException("Claim", claimId);
        }
        return claim;
    }

    static boolean canView(CurrentUser user, Claim claim) {
        if (user.isManager()) {
            return true;
        }
        if (user.isStaff()) {
            return Objects.equals(user.market(), claim.getMarket());
        }
        return claim.isOwnedBy(user.subject());
    }

    static void requireRole(boolean allowed, String message) {
        if (!allowed) {
            throw new AccessDeniedException(message);
        }
    }
}

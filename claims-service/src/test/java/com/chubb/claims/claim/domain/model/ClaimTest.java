package com.chubb.claims.claim.domain.model;

import static com.chubb.claims.claim.ClaimTestData.NOW;
import static com.chubb.claims.claim.ClaimTestData.onlineClaim;
import static com.chubb.claims.claim.ClaimTestData.policy;
import static com.chubb.claims.claim.ClaimTestData.staffRecordedClaim;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chubb.claims.common.error.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClaimTest {

    private static final BigDecimal AMOUNT = new BigDecimal("50000");

    @Test
    void reserveCanOnlyBeProposedWhileUnderReview() {
        Claim claim = onlineClaim(policy("SG", "SGD", "claimant-a"), AMOUNT);

        assertThatThrownBy(() -> claim.proposeReserve(new BigDecimal("10000"), NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectingReserveClearsTheProposal() {
        Claim claim = onlineClaim(policy("SG", "SGD", "claimant-a"), AMOUNT);
        claim.transitionTo(ClaimStatus.UNDER_REVIEW, "staff-sg", NOW);
        claim.proposeReserve(new BigDecimal("10000"), NOW);

        claim.rejectReserve(NOW);

        assertThat(claim.getProposedReserve()).isNull();
        assertThat(claim.getApprovedReserve()).isNull();
    }

    @Test
    void onlineClaimNeverNeedsVerification() {
        Claim claim = onlineClaim(policy("SG", "SGD", "claimant-a"), AMOUNT);

        assertThatThrownBy(() -> claim.assignOwner("claimant-b", NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void staffRecordedClaimCanBeVerifiedOnlyOnce() {
        Claim claim = staffRecordedClaim(policy("SG", "SGD", null), AMOUNT);

        claim.assignOwner("claimant-a", NOW);

        assertThat(claim.isOwnedBy("claimant-a")).isTrue();
        assertThatThrownBy(() -> claim.assignOwner("claimant-b", NOW))
                .isInstanceOf(BusinessRuleException.class);
    }
}

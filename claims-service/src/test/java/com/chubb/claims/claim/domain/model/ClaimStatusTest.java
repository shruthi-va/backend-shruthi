package com.chubb.claims.claim.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ClaimStatusTest {

    @Test
    void reviewCannotBeSkipped() {
        assertThat(ClaimStatus.SUBMITTED.canMoveTo(ClaimStatus.PENDING_DECISION)).isFalse();
        assertThat(ClaimStatus.SUBMITTED.canMoveTo(ClaimStatus.UNDER_REVIEW)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ClaimStatus.class, names = {"REJECTED", "SETTLED", "WITHDRAWN"})
    void terminalStatusesCannotMoveAnywhere(ClaimStatus from) {
        for (ClaimStatus next : ClaimStatus.values()) {
            assertThat(from.canMoveTo(next)).as("%s -> %s", from, next).isFalse();
        }
    }
}

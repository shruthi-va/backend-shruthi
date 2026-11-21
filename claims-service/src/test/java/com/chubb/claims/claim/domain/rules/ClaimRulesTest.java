package com.chubb.claims.claim.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chubb.claims.common.error.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClaimRulesTest {

    private static final LocalDate FILED_ON = LocalDate.of(2025, 6, 15);

    @Test
    void claimFiledOnTheLastDayOfTheWindowIsAccepted() {
        ClaimRules.requireFilingWindow(FILED_ON.minusDays(30), FILED_ON, 30);
    }

    @Test
    void claimFiledAfterTheWindowIsRejected() {
        assertThatThrownBy(() -> ClaimRules.requireFilingWindow(FILED_ON.minusDays(31), FILED_ON, 30))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void lossDateInTheFutureIsRejected() {
        assertThatThrownBy(() -> ClaimRules.requireFilingWindow(FILED_ON.plusDays(1), FILED_ON, 30))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void amountAtThresholdRequiresAudit() {
        assertThat(ClaimRules.requiresAudit(new BigDecimal("1000000"), Optional.of(new BigDecimal("1000000")))).isTrue();
        assertThat(ClaimRules.requiresAudit(new BigDecimal("999999.99"), Optional.of(new BigDecimal("1000000")))).isFalse();
    }

    @Test
    void missingThresholdFailsClosedAndRequiresAudit() {
        assertThat(ClaimRules.requiresAudit(new BigDecimal("1"), Optional.empty())).isTrue();
    }
}

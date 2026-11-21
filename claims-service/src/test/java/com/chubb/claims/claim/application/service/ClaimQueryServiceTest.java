package com.chubb.claims.claim.application.service;

import static com.chubb.claims.claim.ClaimTestData.CLAIMANT_A;
import static com.chubb.claims.claim.ClaimTestData.CLAIMANT_B;
import static com.chubb.claims.claim.ClaimTestData.MANAGER;
import static com.chubb.claims.claim.ClaimTestData.SG_STAFF;
import static com.chubb.claims.claim.ClaimTestData.onlineClaim;
import static com.chubb.claims.claim.ClaimTestData.policy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chubb.claims.claim.application.port.out.ClaimRepository;
import com.chubb.claims.claim.application.query.ClaimFilter;
import com.chubb.claims.claim.application.query.PageResult;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.common.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimQueryServiceTest {

    @Mock
    private ClaimRepository claims;

    private ClaimQueryService service;

    @BeforeEach
    void setUp() {
        service = new ClaimQueryService(claims, new ClaimAccess(claims));
    }

    @Test
    void claimantListingIsForcedToOwnSubject() {
        stubEmptySearch();

        service.list(CLAIMANT_A, filter("claimant-b", "HK", 0, 20));

        assertThat(lastSearch().ownerSubject()).isEqualTo("claimant-a");
    }

    @Test
    void staffListingIsForcedToOwnMarket() {
        stubEmptySearch();

        service.list(SG_STAFF, filter(null, "HK", 0, 20));

        ClaimFilter scoped = lastSearch();
        assertThat(scoped.market()).isEqualTo("SG");
        assertThat(scoped.ownerSubject()).isNull();
    }

    @Test
    void managerKeepsRequestedMarket() {
        stubEmptySearch();

        service.list(MANAGER, filter(null, "HK", 0, 20));

        assertThat(lastSearch().market()).isEqualTo("HK");
    }

    @ParameterizedTest
    @CsvSource({"500,100", "0,1", "20,20"})
    void pageSizeIsClampedToSupportedRange(int requested, int expected) {
        stubEmptySearch();

        service.list(MANAGER, filter(null, null, 0, requested));

        assertThat(lastSearch().size()).isEqualTo(expected);
    }

    @Test
    void negativePageIsTreatedAsFirstPage() {
        stubEmptySearch();

        service.list(MANAGER, filter(null, null, -3, 20));

        assertThat(lastSearch().page()).isZero();
    }

    @Test
    void claimantCannotGetAnotherClaimantsClaim() {
        Claim claim = onlineClaim(policy("SG", "SGD", CLAIMANT_A.subject()), new BigDecimal("500000"));
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.get(CLAIMANT_B, claim.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubEmptySearch() {
        when(claims.search(any(ClaimFilter.class))).thenReturn(new PageResult<>(List.of(), 0, 20, 0));
    }

    private ClaimFilter lastSearch() {
        ArgumentCaptor<ClaimFilter> captor = ArgumentCaptor.forClass(ClaimFilter.class);
        verify(claims).search(captor.capture());
        return captor.getValue();
    }

    private static ClaimFilter filter(String owner, String market, int page, int size) {
        return new ClaimFilter(owner, market, null, null, null, null, page, size);
    }
}

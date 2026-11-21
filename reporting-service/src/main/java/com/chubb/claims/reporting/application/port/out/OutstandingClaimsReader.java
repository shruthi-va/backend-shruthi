package com.chubb.claims.reporting.application.port.out;

import com.chubb.claims.reporting.application.query.OutstandingSummaryRow;
import com.chubb.claims.reporting.application.query.PageResult;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import java.util.List;
import java.util.Set;

public interface OutstandingClaimsReader {

    /** A null market means no filter on that field. */
    List<OutstandingSummaryRow> summarize(Set<ClaimStatus> statuses, String market);

    PageResult<ClaimSnapshot> list(Set<ClaimStatus> statuses, String market, String claimType, int page, int size);
}

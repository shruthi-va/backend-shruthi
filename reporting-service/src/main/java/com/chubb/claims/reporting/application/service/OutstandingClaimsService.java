package com.chubb.claims.reporting.application.service;

import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.reporting.application.port.out.OutstandingClaimsReader;
import com.chubb.claims.reporting.application.query.OutstandingSummaryRow;
import com.chubb.claims.reporting.application.query.PageResult;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Read side of the outstanding-claims dashboard. Managers only; they see every market. */
@Service
public class OutstandingClaimsService {

    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;

    private final OutstandingClaimsReader reader;

    public OutstandingClaimsService(OutstandingClaimsReader reader) {
        this.reader = reader;
    }

    public List<OutstandingSummaryRow> summary(CurrentUser user, String market) {
        requireManager(user);
        return reader.summarize(ClaimStatus.outstandingStatuses(), blankToNull(market));
    }

    public PageResult<ClaimSnapshot> claims(CurrentUser user, String market, String claimType, int page, int size) {
        requireManager(user);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return reader.list(ClaimStatus.outstandingStatuses(), blankToNull(market), blankToNull(claimType),
                safePage, safeSize);
    }

    private static void requireManager(CurrentUser user) {
        if (!user.isManager()) {
            throw new AccessDeniedException("Only managers can view outstanding claims");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

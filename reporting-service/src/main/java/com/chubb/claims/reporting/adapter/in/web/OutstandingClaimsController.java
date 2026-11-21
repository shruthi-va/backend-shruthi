package com.chubb.claims.reporting.adapter.in.web;

import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.reporting.adapter.in.web.dto.OutstandingClaimResponse;
import com.chubb.claims.reporting.adapter.in.web.dto.OutstandingSummaryResponse;
import com.chubb.claims.reporting.adapter.in.web.dto.PageResponse;
import com.chubb.claims.reporting.application.query.PageResult;
import com.chubb.claims.reporting.application.service.OutstandingClaimsService;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboards/outstanding")
@Tag(name = "Outstanding claims", description = "Dashboard of open claims (managers only)")
public class OutstandingClaimsController {

    private final OutstandingClaimsService service;

    public OutstandingClaimsController(OutstandingClaimsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Outstanding claim counts and amounts, grouped by market, status, claim type and currency")
    public List<OutstandingSummaryResponse> summary(
            @RequestParam(required = false) String market, Authentication auth) {
        return service.summary(CurrentUser.from(auth), market).stream()
                .map(OutstandingSummaryResponse::from)
                .toList();
    }

    @GetMapping("/claims")
    @Operation(summary = "Paginated list of outstanding claims, newest update first")
    public PageResponse<OutstandingClaimResponse> claims(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String claimType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        PageResult<ClaimSnapshot> result = service.claims(CurrentUser.from(auth), market, claimType, page, size);
        return new PageResponse<>(
                result.content().stream().map(OutstandingClaimResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}

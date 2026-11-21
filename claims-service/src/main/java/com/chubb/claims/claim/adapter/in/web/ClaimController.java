package com.chubb.claims.claim.adapter.in.web;

import com.chubb.claims.claim.adapter.in.web.dto.ClaimResponse;
import com.chubb.claims.claim.adapter.in.web.dto.DecisionRequest;
import com.chubb.claims.claim.adapter.in.web.dto.FileClaimRequest;
import com.chubb.claims.claim.adapter.in.web.dto.PageResponse;
import com.chubb.claims.claim.adapter.in.web.dto.ReserveRequest;
import com.chubb.claims.claim.adapter.in.web.dto.StaffRecordClaimRequest;
import com.chubb.claims.claim.adapter.in.web.dto.StatusHistoryResponse;
import com.chubb.claims.claim.adapter.in.web.dto.VerifyClaimRequest;
import com.chubb.claims.claim.application.command.FileClaimCommand;
import com.chubb.claims.claim.application.command.StaffRecordClaimCommand;
import com.chubb.claims.claim.application.command.VerifyClaimCommand;
import com.chubb.claims.claim.application.query.ClaimFilter;
import com.chubb.claims.claim.application.query.PageResult;
import com.chubb.claims.claim.application.service.ClaimIntakeService;
import com.chubb.claims.claim.application.service.ClaimLifecycleService;
import com.chubb.claims.claim.application.service.ClaimQueryService;
import com.chubb.claims.claim.domain.model.Claim;
import com.chubb.claims.claim.domain.model.ClaimStatus;
import com.chubb.claims.claim.domain.model.ClaimType;
import com.chubb.claims.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
@Tag(name = "Claims", description = "Claim intake, lifecycle, reserves and queries")
public class ClaimController {

    private final ClaimIntakeService intake;
    private final ClaimLifecycleService lifecycle;
    private final ClaimQueryService queries;

    public ClaimController(ClaimIntakeService intake, ClaimLifecycleService lifecycle, ClaimQueryService queries) {
        this.intake = intake;
        this.lifecycle = lifecycle;
        this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "File an online claim (claimant)")
    public ClaimResponse fileOnline(@Valid @RequestBody FileClaimRequest request, Authentication auth) {
        var command = new FileClaimCommand(request.policyId(), request.claimType(), request.dateOfLoss(),
                request.claimedAmount());
        return ClaimResponse.from(intake.fileOnline(CurrentUser.from(auth), command));
    }

    @PostMapping("/staff-recorded")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a phone claim for a contact (staff)")
    public ClaimResponse staffRecord(@Valid @RequestBody StaffRecordClaimRequest request, Authentication auth) {
        var command = new StaffRecordClaimCommand(request.policyId(), request.contactId(), request.claimType(),
                request.dateOfLoss(), request.claimedAmount());
        return ClaimResponse.from(intake.staffRecord(CurrentUser.from(auth), command));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify a phone claim with policy number and OTP (claimant)")
    public ClaimResponse verify(@PathVariable UUID id, @Valid @RequestBody VerifyClaimRequest request,
                                Authentication auth) {
        var command = new VerifyClaimCommand(request.policyNumber(), request.otp());
        return ClaimResponse.from(intake.verify(CurrentUser.from(auth), id, command));
    }

    @GetMapping
    @Operation(summary = "List claims visible to the caller, with optional filters")
    public PageResponse<ClaimResponse> list(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) ClaimType claimType,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        var filter = new ClaimFilter(null, market, status, claimType,
                filedFrom == null ? null : filedFrom.atStartOfDay(ZoneOffset.UTC).toInstant(),
                filedTo == null ? null : filedTo.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC),
                page, size);
        PageResult<Claim> result = queries.list(CurrentUser.from(auth), filter);
        return PageResponse.of(result, ClaimResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a claim")
    public ClaimResponse get(@PathVariable UUID id, Authentication auth) {
        return ClaimResponse.from(queries.get(CurrentUser.from(auth), id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get the status history of a claim")
    public List<StatusHistoryResponse> history(@PathVariable UUID id, Authentication auth) {
        return queries.history(CurrentUser.from(auth), id).stream().map(StatusHistoryResponse::from).toList();
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw a claim (claimant)")
    public ClaimResponse withdraw(@PathVariable UUID id, Authentication auth) {
        return ClaimResponse.from(lifecycle.withdraw(CurrentUser.from(auth), id));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Start review (staff)")
    public ClaimResponse startReview(@PathVariable UUID id, Authentication auth) {
        return ClaimResponse.from(lifecycle.startReview(CurrentUser.from(auth), id));
    }

    @PostMapping("/{id}/recommendation")
    @Operation(summary = "Recommend a decision (staff)")
    public ClaimResponse recommend(@PathVariable UUID id, Authentication auth) {
        return ClaimResponse.from(lifecycle.recommend(CurrentUser.from(auth), id));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Approve or reject a recommended claim (manager)")
    public ClaimResponse decide(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request,
                                Authentication auth) {
        return ClaimResponse.from(lifecycle.decide(CurrentUser.from(auth), id, request.approve()));
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "Settle an approved claim (staff or manager)")
    public ClaimResponse settle(@PathVariable UUID id, Authentication auth) {
        return ClaimResponse.from(lifecycle.settle(CurrentUser.from(auth), id));
    }

    @PostMapping("/{id}/reserves")
    @Operation(summary = "Propose a reserve (staff)")
    public ClaimResponse proposeReserve(@PathVariable UUID id, @Valid @RequestBody ReserveRequest request,
                                        Authentication auth) {
        BigDecimal amount = request.amount();
        return ClaimResponse.from(lifecycle.proposeReserve(CurrentUser.from(auth), id, amount));
    }

    @PostMapping("/{id}/reserves/approve")
    @Operation(summary = "Approve the proposed reserve (manager)")
    public ClaimResponse approveReserve(@PathVariable UUID id, Authentication auth) {
        return ClaimResponse.from(lifecycle.approveReserve(CurrentUser.from(auth), id));
    }

    @PostMapping("/{id}/reserves/reject")
    @Operation(summary = "Reject the proposed reserve (manager)")
    public ClaimResponse rejectReserve(@PathVariable UUID id, Authentication auth) {
        return ClaimResponse.from(lifecycle.rejectReserve(CurrentUser.from(auth), id));
    }
}

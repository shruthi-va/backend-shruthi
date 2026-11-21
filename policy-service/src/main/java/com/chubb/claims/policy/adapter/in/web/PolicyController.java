package com.chubb.claims.policy.adapter.in.web;

import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.policy.adapter.in.web.dto.AssignOwnerRequest;
import com.chubb.claims.policy.adapter.in.web.dto.CreatePolicyRequest;
import com.chubb.claims.policy.adapter.in.web.dto.PolicyResponse;
import com.chubb.claims.policy.application.command.CreatePolicyCommand;
import com.chubb.claims.policy.application.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@Tag(name = "Policies", description = "Policy catalogue and claimant ownership")
public class PolicyController {

    private final PolicyService policies;

    public PolicyController(PolicyService policies) {
        this.policies = policies;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a policy (manager)")
    public PolicyResponse create(@Valid @RequestBody CreatePolicyRequest request, Authentication auth) {
        var command = new CreatePolicyCommand(request.policyNumber(), request.market(),
                request.productType(), request.currency());
        return PolicyResponse.from(policies.createPolicy(CurrentUser.from(auth), command));
    }

    @GetMapping
    @Operation(summary = "List policies visible to the caller")
    public List<PolicyResponse> list(Authentication auth) {
        return policies.listPolicies(CurrentUser.from(auth)).stream().map(PolicyResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a policy")
    public PolicyResponse get(@PathVariable UUID id, Authentication auth) {
        return PolicyResponse.from(policies.getPolicy(CurrentUser.from(auth), id));
    }

    @PutMapping("/{id}/owner")
    @Operation(summary = "Link a policy to a claimant (manager)")
    public PolicyResponse assignOwner(@PathVariable UUID id, @Valid @RequestBody AssignOwnerRequest request,
                                      Authentication auth) {
        return PolicyResponse.from(policies.assignOwner(CurrentUser.from(auth), id, request.ownerSubject()));
    }
}

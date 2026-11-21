package com.chubb.claims.policy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chubb.claims.common.error.BusinessRuleException;
import com.chubb.claims.common.error.ResourceNotFoundException;
import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.policy.application.command.CreatePolicyCommand;
import com.chubb.claims.policy.application.port.out.EventOutbox;
import com.chubb.claims.policy.application.port.out.PolicyRepository;
import com.chubb.claims.policy.domain.model.Policy;
import com.chubb.claims.policy.domain.model.PolicyStatus;
import com.chubb.claims.policy.domain.model.ProductType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    private static final CurrentUser MANAGER = new CurrentUser("mgr-1", "SG", Set.of(CurrentUser.MANAGER));
    private static final CurrentUser SG_STAFF = new CurrentUser("staff-sg", "SG", Set.of(CurrentUser.CLAIMS_STAFF));
    private static final CurrentUser HK_STAFF = new CurrentUser("staff-hk", "HK", Set.of(CurrentUser.CLAIMS_STAFF));
    private static final CurrentUser CLAIMANT_A = new CurrentUser("claimant-a", "SG", Set.of(CurrentUser.CLAIMANT));
    private static final CurrentUser CLAIMANT_B = new CurrentUser("claimant-b", "SG", Set.of(CurrentUser.CLAIMANT));

    @Mock
    private PolicyRepository policies;

    @Mock
    private EventOutbox outbox;

    private PolicyService service;

    @BeforeEach
    void setUp() {
        service = new PolicyService(policies, outbox);
    }

    @Test
    void managerCreatesPolicyAndPublishesPolicyChanged() {
        when(policies.findByPolicyNumber("POL-1")).thenReturn(Optional.empty());
        when(policies.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        Policy created = service.createPolicy(MANAGER,
                new CreatePolicyCommand("POL-1", "SG", ProductType.MOTOR, "SGD"));

        assertThat(created.policyNumber()).isEqualTo("POL-1");
        assertThat(created.ownerSubject()).isNull();
        assertThat(created.status()).isEqualTo(PolicyStatus.ACTIVE);
        verify(outbox).append(eq("policies.changed"), eq(created.id().toString()),
                argThat(event -> "PolicyChanged".equals(event.eventType()) && "SG".equals(event.market())));
    }

    @Test
    void nonManagerCannotCreatePolicy() {
        assertThatThrownBy(() -> service.createPolicy(SG_STAFF,
                new CreatePolicyCommand("POL-2", "SG", ProductType.MOTOR, "SGD")))
                .isInstanceOf(AccessDeniedException.class);

        verify(policies, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void duplicatePolicyNumberIsRejected() {
        when(policies.findByPolicyNumber("POL-1")).thenReturn(Optional.of(newPolicy("SG", null)));

        assertThatThrownBy(() -> service.createPolicy(MANAGER,
                new CreatePolicyCommand("POL-1", "SG", ProductType.MOTOR, "SGD")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("POL-1");

        verify(policies, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void managerAssignsOwnerAndPublishesNewRevision() {
        Policy policy = newPolicy("SG", null);
        when(policies.findById(policy.id())).thenReturn(Optional.of(policy));
        when(policies.save(policy)).thenReturn(policy);

        Policy updated = service.assignOwner(MANAGER, policy.id(), "claimant-a");

        assertThat(updated.ownerSubject()).isEqualTo("claimant-a");
        assertThat(updated.revision()).isEqualTo(2L);
        verify(outbox).append(eq("policies.changed"), eq(policy.id().toString()),
                argThat(event -> "PolicyChanged".equals(event.eventType())));
    }

    @Test
    void claimantCannotAssignOwner() {
        Policy policy = newPolicy("SG", null);

        assertThatThrownBy(() -> service.assignOwner(CLAIMANT_A, policy.id(), "claimant-a"))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(policies, outbox);
    }

    @Test
    void assigningSecondOwnerIsRejectedAndNothingPublished() {
        Policy policy = newPolicy("SG", "claimant-a");
        when(policies.findById(policy.id())).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> service.assignOwner(MANAGER, policy.id(), "claimant-b"))
                .isInstanceOf(BusinessRuleException.class);

        verify(policies, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void claimantCanReadOwnPolicyButNotAnotherClaimantsPolicy() {
        Policy policy = newPolicy("SG", "claimant-a");
        when(policies.findById(policy.id())).thenReturn(Optional.of(policy));

        assertThat(service.getPolicy(CLAIMANT_A, policy.id())).isSameAs(policy);
        assertThatThrownBy(() -> service.getPolicy(CLAIMANT_B, policy.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void staffCanReadPoliciesOnlyInTheirMarket() {
        Policy sgPolicy = newPolicy("SG", null);
        when(policies.findById(sgPolicy.id())).thenReturn(Optional.of(sgPolicy));

        assertThat(service.getPolicy(SG_STAFF, sgPolicy.id())).isSameAs(sgPolicy);
        assertThatThrownBy(() -> service.getPolicy(HK_STAFF, sgPolicy.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listIsScopedByRole() {
        Policy sgPolicy = newPolicy("SG", null);
        when(policies.findAll()).thenReturn(List.of(sgPolicy, newPolicy("HK", null)));
        when(policies.findByMarket("SG")).thenReturn(List.of(sgPolicy));
        when(policies.findByOwnerSubject("claimant-a")).thenReturn(List.of());

        assertThat(service.listPolicies(MANAGER)).hasSize(2);
        assertThat(service.listPolicies(SG_STAFF)).containsExactly(sgPolicy);
        assertThat(service.listPolicies(CLAIMANT_A)).isEmpty();
    }

    private static Policy newPolicy(String market, String ownerSubject) {
        return Policy.restore(UUID.randomUUID(), "POL-" + UUID.randomUUID(), market, ProductType.MOTOR,
                "SGD", ownerSubject, PolicyStatus.ACTIVE, 1L, Instant.now());
    }
}

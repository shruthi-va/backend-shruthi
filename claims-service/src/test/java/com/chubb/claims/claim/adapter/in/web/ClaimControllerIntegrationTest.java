package com.chubb.claims.claim.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chubb.claims.claim.application.event.PolicyChangedPayload;
import com.chubb.claims.claim.application.service.PolicyProjectionService;
import com.chubb.claims.claim.support.AbstractIntegrationTest;
import com.chubb.claims.common.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ClaimControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PolicyProjectionService projections;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
    }

    @Test
    void claimantFilesOnlineClaimAndCannotSeeAnotherClaimantsClaim() throws Exception {
        String owner = newSubject();
        String other = newSubject();
        UUID policyId = seedPolicy(owner, "POL-ONLINE");

        UUID claimId = fileOnline(owner, policyId);

        mvc.perform(get("/api/claims/{id}", claimId).with(as(owner, IN_MARKET, CurrentUser.CLAIMANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.ownerSubject").value(owner));
        mvc.perform(get("/api/claims/{id}", claimId).with(as(other, IN_MARKET, CurrentUser.CLAIMANT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void claimantCannotFileAgainstAPolicyTheyDoNotHold() throws Exception {
        UUID policyId = seedPolicy(newSubject(), "POL-NOT-MINE");

        mvc.perform(post("/api/claims")
                        .with(as(newSubject(), IN_MARKET, CurrentUser.CLAIMANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineBody(policyId, LocalDate.now(ZoneOffset.UTC).minusDays(2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFilingsAreRejected() throws Exception {
        String owner = newSubject();
        UUID policyId = seedPolicy(owner, "POL-INVALID");

        mvc.perform(post("/api/claims")
                        .with(as(owner, IN_MARKET, CurrentUser.CLAIMANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyId":"%s","claimType":"MOTOR","dateOfLoss":"%s"}
                                """.formatted(policyId, LocalDate.now(ZoneOffset.UTC).minusDays(2))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/claims")
                        .with(as(owner, IN_MARKET, CurrentUser.CLAIMANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineBody(policyId, LocalDate.now(ZoneOffset.UTC).minusDays(60))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void staffRecordedClaimIsLimitedToTheStaffMarket() throws Exception {
        UUID policyId = seedPolicy(newSubject(), "POL-PHONE");
        String body = staffBody(policyId);

        mvc.perform(post("/api/claims/staff-recorded")
                        .with(as("staff-hk", "HK", CurrentUser.CLAIMS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/claims/staff-recorded")
                        .with(as("staff-in", IN_MARKET, CurrentUser.CLAIMS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("STAFF_RECORDED"));
    }

    @Test
    void phoneClaimIsLinkedToClaimantOnlyAfterOtpVerification() throws Exception {
        String claimant = newSubject();
        String policyNumber = "POL-PHONE-" + UUID.randomUUID().toString().substring(0, 6);
        UUID policyId = seedPolicy(newSubject(), policyNumber);
        UUID claimId = recordByStaff(policyId);

        mvc.perform(post("/api/claims/{id}/verify", claimId)
                        .with(as(claimant, IN_MARKET, CurrentUser.CLAIMANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyNumber\":\"%s\",\"otp\":\"000000\"}".formatted(policyNumber)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/claims/{id}/verify", claimId)
                        .with(as(claimant, IN_MARKET, CurrentUser.CLAIMANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyNumber\":\"%s\",\"otp\":\"123456\"}".formatted(policyNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerSubject").value(claimant));
    }

    @Test
    void lifecycleRunsFromReviewToSettlementWithRoleChecks() throws Exception {
        String owner = newSubject();
        UUID claimId = fileOnline(owner, seedPolicy(owner, "POL-LIFECYCLE"));

        mvc.perform(post("/api/claims/{id}/review", claimId).with(as("staff-in", IN_MARKET, CurrentUser.CLAIMS_STAFF)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        mvc.perform(post("/api/claims/{id}/recommendation", claimId)
                        .with(as("staff-in", IN_MARKET, CurrentUser.CLAIMS_STAFF)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_DECISION"));
        mvc.perform(post("/api/claims/{id}/decision", claimId)
                        .with(as(owner, IN_MARKET, CurrentUser.CLAIMANT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approve\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/claims/{id}/decision", claimId)
                        .with(as("manager-1", IN_MARKET, CurrentUser.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approve\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/claims/{id}/settle", claimId).with(as("staff-in", IN_MARKET, CurrentUser.CLAIMS_STAFF)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SETTLED"));
        mvc.perform(get("/api/claims/{id}/history", claimId).with(as("manager-1", IN_MARKET, CurrentUser.MANAGER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void reserveNeedsManagerApproval() throws Exception {
        String owner = newSubject();
        UUID claimId = fileOnline(owner, seedPolicy(owner, "POL-RESERVE"));

        mvc.perform(post("/api/claims/{id}/reserves", claimId)
                        .with(as("staff-in", IN_MARKET, CurrentUser.CLAIMS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":3000.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proposedReserve").value(3000.0));
        mvc.perform(post("/api/claims/{id}/reserves/approve", claimId)
                        .with(as("staff-in", IN_MARKET, CurrentUser.CLAIMS_STAFF)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/claims/{id}/reserves/approve", claimId)
                        .with(as("manager-1", IN_MARKET, CurrentUser.MANAGER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approvedReserve").value(3000.0));
    }

    @Test
    void claimListShowsOnlyTheCallersOwnClaims() throws Exception {
        String owner = newSubject();
        fileOnline(owner, seedPolicy(owner, "POL-LIST"));

        mvc.perform(get("/api/claims").with(as(owner, IN_MARKET, CurrentUser.CLAIMANT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/claims").with(as(newSubject(), IN_MARKET, CurrentUser.CLAIMANT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    private UUID seedPolicy(String owner, String policyNumber) {
        UUID policyId = UUID.randomUUID();
        projections.apply(UUID.randomUUID(), new PolicyChangedPayload(
                policyId, policyNumber, IN_MARKET, "MOTOR", INR, owner, "ACTIVE", 1L));
        return policyId;
    }

    private UUID fileOnline(String owner, UUID policyId) throws Exception {
        String json = mvc.perform(post("/api/claims")
                        .with(as(owner, IN_MARKET, CurrentUser.CLAIMANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineBody(policyId, LocalDate.now(ZoneOffset.UTC).minusDays(2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(json).get("id").asText());
    }

    private UUID recordByStaff(UUID policyId) throws Exception {
        String json = mvc.perform(post("/api/claims/staff-recorded")
                        .with(as("staff-in", IN_MARKET, CurrentUser.CLAIMS_STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content(staffBody(policyId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(json).get("id").asText());
    }

    private static String onlineBody(UUID policyId, LocalDate dateOfLoss) {
        return """
                {"policyId":"%s","claimType":"MOTOR","dateOfLoss":"%s","claimedAmount":5000.00}
                """.formatted(policyId, dateOfLoss);
    }

    private static String staffBody(UUID policyId) {
        return """
                {"policyId":"%s","contactId":"%s","claimType":"MOTOR","dateOfLoss":"%s","claimedAmount":5000.00}
                """.formatted(policyId, UUID.randomUUID(), LocalDate.now(ZoneOffset.UTC).minusDays(2));
    }

    private static String newSubject() {
        return "claimant-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

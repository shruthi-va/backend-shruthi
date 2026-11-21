package com.chubb.claims.policy.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chubb.claims.policy.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

class PolicyControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void unauthenticatedCallerIsRejected() throws Exception {
        mvc.perform(get("/api/policies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void claimantCannotCreatePolicyAndDuplicateNumberIsRejected() throws Exception {
        String number = newPolicyNumber();

        mvc.perform(post("/api/policies").with(as("claimant-x", IN_MARKET, "CLAIMANT"))
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson(number, IN_MARKET, INR)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/policies").with(as("mgr-dup", IN_MARKET, "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson(number, IN_MARKET, INR)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/policies").with(as("mgr-dup", IN_MARKET, "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson(number, IN_MARKET, INR)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void assignedClaimantSeesOnlyTheirOwnPolicy() throws Exception {
        String number = newPolicyNumber();
        String owner = "claimant-" + UUID.randomUUID();
        String policyId = createPolicy(number, IN_MARKET);

        mvc.perform(put("/api/policies/{id}/owner", policyId).with(as("mgr-own", IN_MARKET, "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerSubject\":\"" + owner + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/policies").with(as(owner, IN_MARKET, "CLAIMANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].policyNumber").value(number));

        mvc.perform(get("/api/policies/{id}", policyId).with(as("claimant-other-" + owner, IN_MARKET, "CLAIMANT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCannotReadPoliciesOutsideTheirMarket() throws Exception {
        String inPolicy = createPolicy(newPolicyNumber(), IN_MARKET);
        String hkPolicy = createPolicy(newPolicyNumber(), HK_MARKET);

        mvc.perform(get("/api/policies/{id}", inPolicy).with(as("staff-in", IN_MARKET, "CLAIMS_STAFF")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/policies/{id}", hkPolicy).with(as("staff-in", IN_MARKET, "CLAIMS_STAFF")))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffRecordsContactOnlyInTheirOwnMarket() throws Exception {
        String contact = """
                {"market":"%s","fullName":"Asha Rao","email":"asha@example.com","phone":"+91 98765 43210",
                 "emailOptIn":true,"smsOptIn":false}
                """;

        mvc.perform(post("/api/contacts").with(as("staff-in", IN_MARKET, "CLAIMS_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(contact.formatted(IN_MARKET)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/contacts").with(as("staff-in", IN_MARKET, "CLAIMS_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(contact.formatted(HK_MARKET)))
                .andExpect(status().isForbidden());
    }

    @Test
    void claimantMaintainsOwnContactButStaffCannot() throws Exception {
        String body = """
                {"fullName":"Ravi Kumar","email":"ravi@example.com","phone":"+91 90000 11111",
                 "emailOptIn":true,"smsOptIn":true}
                """;

        mvc.perform(put("/api/contacts/me").with(as("claimant-" + UUID.randomUUID(), IN_MARKET, "CLAIMANT"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mvc.perform(put("/api/contacts/me").with(as("staff-in", IN_MARKET, "CLAIMS_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    private String createPolicy(String number, String market) throws Exception {
        MvcResult result = mvc.perform(post("/api/policies").with(as("mgr-seed", market, "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson(number, market, INR)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}

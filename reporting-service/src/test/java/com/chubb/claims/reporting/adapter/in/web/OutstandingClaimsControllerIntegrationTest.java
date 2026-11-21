package com.chubb.claims.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.reporting.application.port.out.ClaimSnapshotRepository;
import com.chubb.claims.reporting.domain.model.ClaimSnapshot;
import com.chubb.claims.reporting.domain.model.ClaimStatus;
import com.chubb.claims.reporting.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Each test runs in a transaction that is rolled back, so seeded snapshots do not leak between tests. */
@Transactional
class OutstandingClaimsControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String NZ = "NZ";
    private static final String SUMMARY = "/api/dashboards/outstanding/summary";
    private static final String CLAIMS = "/api/dashboards/outstanding/claims";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClaimSnapshotRepository snapshots;

    @BeforeEach
    void seedClaims() {
        snapshot(ClaimStatus.SUBMITTED, "MOTOR", "1000.00");
        snapshot(ClaimStatus.UNDER_REVIEW, "PROPERTY", "2500.00");
        snapshot(ClaimStatus.SETTLED, "MOTOR", "300.00");
        snapshot(ClaimStatus.REJECTED, "MOTOR", "400.00");
    }

    @Test
    void unauthenticatedCallerIsRejected() throws Exception {
        mockMvc.perform(get(SUMMARY).param("market", NZ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotViewDashboard() throws Exception {
        mockMvc.perform(get(CLAIMS).param("market", NZ).with(as("staff-1", NZ, CurrentUser.CLAIMS_STAFF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerSeesOnlyOutstandingClaimsInSummary() throws Exception {
        JsonNode rows = json(mockMvc.perform(get(SUMMARY).param("market", NZ)
                .with(as("mgr-1", NZ, CurrentUser.MANAGER))));

        assertThat(rows).hasSize(2);
        assertThat(findRow(rows, "SUBMITTED").path("claimCount").asLong()).isEqualTo(1);
        assertThat(findRow(rows, "UNDER_REVIEW").path("totalClaimedAmount").decimalValue())
                .isEqualByComparingTo("2500.00");
    }

    @Test
    void managerGetsPagedOutstandingClaimsFilteredByType() throws Exception {
        JsonNode page = json(mockMvc.perform(get(CLAIMS)
                .param("market", NZ)
                .param("claimType", "PROPERTY")
                .with(as("mgr-1", NZ, CurrentUser.MANAGER))));

        assertThat(page.path("totalElements").asLong()).isEqualTo(1);
        assertThat(page.path("content").get(0).path("status").asText()).isEqualTo("UNDER_REVIEW");
    }

    private void snapshot(ClaimStatus status, String claimType, String amount) {
        snapshots.save(new ClaimSnapshot(UUID.randomUUID(), "CLM-" + UUID.randomUUID().toString().substring(0, 8),
                NZ, status, claimType, new BigDecimal(amount), "NZD", false, Instant.now()));
    }

    private JsonNode json(ResultActions actions) throws Exception {
        String body = actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static JsonNode findRow(JsonNode rows, String status) {
        for (JsonNode row : rows) {
            if (status.equals(row.path("status").asText())) {
                return row;
            }
        }
        throw new AssertionError("No summary row for status " + status);
    }
}

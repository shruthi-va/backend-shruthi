# Technical Design Document: Claims Management Platform

| Field | Value |
|---|---|
| Document | Technical Design Document (TDD) |
| Related | [PRD.md](../PRD.md), diagrams in this folder |
| Status | Draft for review. Not yet implemented. |
| Date | 2026-11-21 |
| Version | 0.1 |

Requirements and business rules (BR-1 to BR-10) are defined in [PRD.md](../PRD.md). This document describes how they are built. Items marked **Proposed** need confirmation. Items marked **Open** are listed in section 15.

---

## 1. Overview

A set of Spring Boot 3.3 microservices on Java 17, built with Maven. Each service has its own H2 database. Services communicate through REST for synchronous queries and Apache Kafka for events. Keycloak issues JWTs. Services act as OAuth2 resource servers. Swagger UI documents and verifies every endpoint. The system runs locally, without Docker.

## 2. Services and ports

| Service | Port (local, proposed) | Database | Responsibility |
|---|---|---|---|
| `common` | n/a (library) | n/a | Security config, `ProblemDetail` error handling, `DomainEvent` envelope, OpenAPI config |
| `policy-service` | 8081 | H2 `policydb` | Policies, policy ownership, claimant contact details |
| `claims-service` | 8082 | H2 `claimsdb` | Claims, lifecycle, status history, reserves, recommendations (assessment), queues, outbox, local policy projection |
| `reporting-service` | 8084 | H2 `reportingdb` | Read model for manager dashboards, built from Kafka events |

Each service is an independent Spring Boot application under the parent POM `claims-platform`. Diagrams: [architecture.md](./architecture.md).

**Decided:** `assessment-service` from the PRD is folded into `claims-service` for the first release. Recommendations are a claims-service resource. Split it out later only if its load or team ownership requires it.

## 3. Domain model

Field types are conceptual. Java types are chosen during implementation.

### 3.1 policy-service

**Policy**
- `id` (UUID), `policyNumber` (unique), `market` (code), `productType` (MOTOR or PROPERTY)
- `ownerSubject` (Keycloak `sub` of the claimant who holds the policy; nullable until linked)
- `status` (ACTIVE, EXPIRED, CANCELLED), `startDate`, `endDate`
- `version` (optimistic locking)

**ClaimantContact** (owned by policy-service, decided as D2)
- `id`, `contactId` (unique, issued by staff for phone/email reporters)
- `ownerSubject` (nullable until verified)
- `phone`, `email` (personal data; never sent in Kafka)

### 3.2 claims-service

**Claim**
- `id` (UUID), `claimNumber` (unique, generated, human-readable)
- `policyId` (reference to policy-service; one policy per claim, BR-1)
- `market` (code), `claimType` (MOTOR or PROPERTY)
- `ownerSubject` (nullable for unverified phone/email claims)
- `contactId` (nullable; set for staff-recorded claims)
- `status` (see 4)
- `dateOfLoss` (date), `filedAt` (timestamp)
- `claimedAmount` (decimal, 2 dp) and `currency` (ISO 4217, from market config)
- `auditFlag` (boolean, BR-4)
- `assignedTo` (staff subject, nullable)
- `version` (optimistic locking, see 9.3)

**PolicyProjection** (local copy of policy ownership, fed by `policies.changed`): `policyId` (PK), `policyNumber`, `market`, `ownerSubject` (nullable), `status`, `version`, `updatedAt`.

**StatusHistory**: `id`, `claimId`, `fromStatus`, `toStatus`, `actorSubject`, `reason`, `occurredAt`. Append-only.

**Reserve**: `id`, `claimId`, `amount`, `currency`, `status` (PROPOSED, APPROVED, REJECTED), `proposedBy`, `decidedBy`, `justification`, `createdAt`, `decidedAt`.

**Recommendation**: `id`, `claimId`, `recommendedBy`, `outcome` (APPROVE or REJECT), `notes`, `createdAt`. At most one active recommendation per claim.

**OutboxEvent**: `id`, `aggregateId` (claimId), `eventType`, `payload` (JSON), `createdAt`, `publishedAt` (nullable).

**Market configuration** (application config, not a table in the first release): market code, currency code, and per-currency audit threshold. The PRD does not yet define how the threshold applies to non-INR markets (Q1, open).

### 3.3 reporting-service

Read model only. Rebuilt from events. Table `claim_snapshot` (migration `V1__init.sql`).
- **ClaimSnapshot**: `claimId` (PK), `claimNumber`, `market`, `status`, `claimType`, `claimedAmount`, `currency`, `auditFlag`, `updatedAt`.
- **ProcessedEvent**: `eventId` (unique), `processedAt`. Used for idempotency.

Reserve snapshots are not built yet. Reserve reporting is deferred with the exposure dashboard (section 10).

Aggregates for the dashboards are computed by query, not stored.

## 4. Claim lifecycle implementation

- Statuses: SUBMITTED, UNDER_REVIEW, PENDING_DECISION, APPROVED, REJECTED, SETTLED, WITHDRAWN.
- One class, `ClaimStateMachine`, holds the allowed transitions map. All status changes go through it. Any other transition throws `BusinessRuleException` (HTTP 422).
- Allowed transitions:
  - SUBMITTED → UNDER_REVIEW (staff)
  - SUBMITTED → WITHDRAWN (claimant owner; **Q2 open**)
  - UNDER_REVIEW → PENDING_DECISION (staff recommendation)
  - PENDING_DECISION → APPROVED or REJECTED (manager)
  - APPROVED → SETTLED (manager)
- Each transition writes one `StatusHistory` row and one `OutboxEvent` in the same transaction.
- Only transitions where the status changes produce `ClaimStatusChanged`.

## 5. Business rule implementation

| Rule | Implementation |
|---|---|
| BR-1 one policy per claim | `policyId` is a single non-null reference on `Claim`. |
| BR-2 policy ownership at submission | `claims-service` checks its local `PolicyProjection` (see 7.3). If no policy with that ID exists, or its `ownerSubject` differs from the token `sub`, submission is rejected with 422. |
| BR-3 30-day deadline | `FilingDeadline.isMet(dateOfLoss, filedAt)`: `filedAt <= dateOfLoss + 30 days`. Violation returns 422. |
| BR-4 threshold and audit flag | At submission and on every amount change, `auditFlag = claimedAmount >= threshold(currency)`. Threshold comes from market configuration. |
| BR-5 manager approves any amount | No amount check on approval. `auditFlag` is only a filter for reporting. |
| BR-6 manager finalizes | `POST /claims/{id}/decision` requires `MANAGER`. Staff use `POST /claims/{id}/recommendation`. |
| BR-7 reserves | `Reserve` is created as PROPOSED by staff. Only `MANAGER` can approve or reject it. |
| BR-8 currency | Amounts stored as given, with currency. No conversion code. Comparisons only within one currency. |
| BR-9 shared rules | No per-market rule branches. Only `market` and `currency` differ. |
| BR-10 withdrawal | Allowed only from SUBMITTED, owner only. |

## 6. Security

### 6.1 Identity and tokens

- Keycloak realm `claims` (**proposed**, Q4). Issuer `http://localhost:8180/realms/claims`.
- Each service validates JWT signatures using the issuer's JWKS. Services use `spring-boot-starter-oauth2-resource-server`.
- Roles come from `realm_access.roles`: `CLAIMANT`, `CLAIMS_STAFF`, `MANAGER`. `KeycloakRoleConverter` (in `common`) maps them to `ROLE_*`.
- Staff `market` comes from the custom token claim named by Keycloak mapper (**Open**, Q3). The `common` module reads it into a `CurrentUser` value object.

### 6.2 Authorization layers

1. **URL rules** in `SecurityConfig`: public paths are Swagger and Actuator health. All other paths require authentication.
2. **Method rules** with `@PreAuthorize`, for example `hasRole('MANAGER')`.
3. **Data rules** in the service layer:
   - Claimant: a claim is visible only when `ownerSubject == token.sub`. Otherwise `ResourceNotFoundException` (404), so claim existence is not revealed.
   - Staff: queries filter by `market == token.market`. Access to another market returns 404.
   - Manager: no market filter.

### 6.3 Data protection

- No names, emails, or phone numbers in Kafka payloads or logs.
- Logs use claim ID and event ID only.
- Secrets are not committed. Keycloak client secrets and mail credentials come from environment variables.

## 7. APIs

All responses use JSON. Errors use RFC 7807 `ProblemDetail` (from `GlobalExceptionHandler`). Lists are paginated: `page` (default 0), `size` (default 20, max 100). Every API is listed in Swagger.

### 7.1 claims-service

| Method and path | Role | Purpose |
|---|---|---|
| `POST /claims` | CLAIMANT | File a claim against an owned policy (online). |
| `POST /claims/staff-recorded` | CLAIMS_STAFF | Record a phone or email claim against a `contactId`. |
| `POST /claims/{id}/verify` | CLAIMANT | Verify a staff-recorded claim with policy number and OTP. Sets `ownerSubject`. |
| `GET /claims` | CLAIMANT (own), CLAIMS_STAFF (market), MANAGER (all) | List claims. Filters: status, claimType, dateRange. |
| `GET /claims/{id}` | same as above | Claim detail with status history. |
| `GET /claims/{id}/history` | same as above | Status history. |
| `POST /claims/{id}/withdraw` | CLAIMANT (owner) | Withdraw from SUBMITTED (BR-10). |
| `POST /claims/{id}/assign` | CLAIMS_STAFF (market), MANAGER | Assign to a staff member. Changes queue membership. |
| `POST /claims/{id}/review` | CLAIMS_STAFF | SUBMITTED → UNDER_REVIEW. |
| `POST /claims/{id}/recommendation` | CLAIMS_STAFF | Record recommendation. UNDER_REVIEW → PENDING_DECISION. |
| `POST /claims/{id}/decision` | MANAGER | APPROVED or REJECTED from PENDING_DECISION. |
| `POST /claims/{id}/settle` | MANAGER | APPROVED → SETTLED. |
| `POST /claims/{id}/reserves` | CLAIMS_STAFF | Propose a reserve. |
| `POST /claims/{id}/reserves/{reserveId}/approve` | MANAGER | Approve a reserve. |
| `POST /claims/{id}/reserves/{reserveId}/reject` | MANAGER | Reject a reserve. |
| `GET /queues/me` | CLAIMS_STAFF | Staff's assigned claims, plus unassigned claims in their market. |

Staff-recorded claims are visible only to staff until verified.

### 7.2 policy-service

| Method and path | Role | Purpose |
|---|---|---|
| `POST /policies` | MANAGER | Create a policy. |
| `PUT /policies/{id}` | MANAGER | Update a policy. |
| `GET /policies/{id}` | CLAIMANT (own), CLAIMS_STAFF (market), MANAGER | Read a policy. |
| `GET /policies` | CLAIMANT (own), CLAIMS_STAFF (market), MANAGER | List policies. |
| `POST /claimants/contacts` | CLAIMS_STAFF | Record a contact for a phone or email reporter. |
| `POST /claimants/verify-policy` | CLAIMANT | Verify policy number and OTP for claim linking (see 7.4). |
| `PUT /claimants/me/contact` | CLAIMANT | Update own contact details. |

### 7.3 Local policy projection (decided)

`claims-service` keeps a `PolicyProjection` table (see 3.2). policy-service publishes `policies.changed` on every create or update, and claims-service consumes it into the projection. Ownership checks (BR-2) read only the local table, so submission does not depend on policy-service being available at request time.

The projection can lag the source by a few seconds. Consumers apply an event only if its `version` is newer than the stored `version`, so out-of-order events cannot overwrite newer data. A claim whose policy is not yet in the projection is rejected with 422, and the claimant can retry.

### 7.4 Phone-claim verification (option A)

1. Staff records the claim with `contactId`. `ownerSubject` is null.
2. Claimant calls `POST /claims/{id}/verify` with policy number and an OTP.
3. policy-service checks the OTP and that the policy number matches the claim's policy. On success, it returns the verified subject to claims-service, which sets `ownerSubject`.
4. The OTP delivery channel is **Open** (Q5). Until it is chosen, verification can only be tested with a stub OTP service.

## 8. Events and Kafka

### 8.1 Topics

| Topic | Producer | Consumers | Key | Partitions (proposed) |
|---|---|---|---|---|
| `claims.status-changed` | claims-service | reporting-service | `claimId` | 6 |
| `claims.reserve-changed` | claims-service | reporting-service | `claimId` | 6 |
| `policies.changed` | policy-service | claims-service | `policyId` | 3 |

Topic names are fixed by the PRD. Partition counts are for local testing.

### 8.2 Envelope

`DomainEvent<T>` in `common`:
- `eventId` (UUID), `eventType`, `aggregateId`, `market`, `occurredAt`, `payload`
- Header `correlationId` (propagated from the HTTP request).
- Schema version field `schemaVersion` (starts at 1).

### 8.3 ClaimStatusChanged payload

`claimId`, `claimNumber`, `market`, `fromStatus`, `toStatus`, `ownerSubject` (token sub; not personal data), `changedAt`. For reporting-service, also `claimType`, `claimedAmount`, `currency`, `auditFlag`, so the snapshot can be built without calling claims-service. None of these is personal data.

### 8.4 Transactional outbox

- The claim change and its `OutboxEvent` are written in one database transaction.
- `OutboxRelay` (scheduled, every 500 ms, configurable) reads unpublished rows in order, publishes them with the record key, and sets `publishedAt` after the broker acknowledges.
- Delivery is at-least-once. Duplicates are handled by consumers (9.2).
- Producer settings: `acks=all`, `enable.idempotence=true`.

### 8.5 Consumers

- Consumer group: `reporting-service`.
- Offsets committed after the local database transaction completes.
- Retry: 3 attempts with exponential backoff. Records that still fail go to `<topic>.DLT` through `DefaultErrorHandler`.

## 9. Consistency and reliability

### 9.1 Idempotency

- Consumers check `ProcessedEvent.eventId` inside the same transaction as their side effects.

### 9.2 Duplicate handling

Repeated events are skipped if `eventId` is already in `ProcessedEvent`. Events that arrive out of order for the same claim are ignored when `occurredAt` is older than the last processed event for that claim (reporting-service only, using `ClaimStatusSnapshot.updatedAt`).

### 9.3 Concurrency

- `@Version` on `Claim` and `Policy`. Concurrent updates return 409 Conflict.
- Staff assignment and manager decisions are protected by this check.

## 10. Reporting

- `reporting-service` consumes `claims.status-changed` and maintains the `claim_snapshot` read model. It has no REST call to claims-service.
- "Outstanding" means non-terminal: SUBMITTED, UNDER_REVIEW, PENDING_DECISION, APPROVED. SETTLED, REJECTED and WITHDRAWN are excluded.
- Dashboard endpoints (MANAGER only; staff get 403):
  - `GET /api/dashboards/outstanding/summary`: count and total claimed amount by market, status, claimType and currency. Currencies are not summed across markets, so totals are per currency. Optional `market` filter.
  - `GET /api/dashboards/outstanding/claims`: paginated list of outstanding claims, newest update first. Optional `market` and `claimType` filters; `page` (from 0) and `size` (clamped to 1 to 100, default 20).
- Deferred, not built yet (the user asked only for outstanding claims): `reserves` consumption from `claims.reserve-changed`, `GET /dashboards/exposure` and `GET /dashboards/audit`.
- Freshness target: under 10 seconds behind the source change (PRD section 14). Not yet measured.

## 11. Persistence and migrations

- H2 per service in file mode under `./data/<service>/` (proposed) so data survives restarts. In-memory mode for tests.
- Schema managed by Flyway: `V1__init.sql` for each service, then incremental versions.
- H2 console is enabled only in the `local` profile.
- Money columns use `DECIMAL(18,2)`.
- Indexes:
  - `claim(ownerSubject, status)`
  - `claim(market, status)`
  - `claim(policyId)`
  - `outbox_event(publishedAt, createdAt)`
  - `processed_event(eventId)` unique

## 12. Error handling

| Situation | HTTP | Example |
|---|---|---|
| Validation failure | 400 | Missing `claimedAmount` |
| Not authenticated | 401 | No or invalid token |
| Role not allowed | 403 | Claimant calls `/decision` |
| Not visible to caller | 404 | Claimant asks for another claimant's claim |
| Business rule violation | 422 | Filed after 30 days; invalid transition |
| Concurrent update | 409 | Stale `version` |
| Dependency unavailable | 503 | policy-service timeout during submission |

All errors return `ProblemDetail` with a `correlationId` field.

## 13. Testing approach

| Level | Tool | Covers |
|---|---|---|
| Unit | JUnit 5, AssertJ | `ClaimStateMachine`, deadline check, threshold check, reserve rules |
| Repository | `@DataJpaTest` with H2 | Queries, market filters, unique constraints |
| Web and security | `@WebMvcTest` with `spring-security-test` `jwt()` | 401, 403, 404 for claimant access, role checks |
| Messaging | `@EmbeddedKafka` | Outbox publish, consumer idempotency, DLT routing |
| End to end | `@SpringBootTest` across services where feasible | Submission to reporting snapshot |

Tests map to business rules: each BR-1 to BR-10 has at least one automated test (PRD section 14).

## 14. Local runtime

- Java 17 and Maven required.
- Keycloak runs as a local install on `localhost:8180`, realm `claims`.
- Kafka runs locally in KRaft mode on `localhost:9092`.
- Start order: Keycloak, Kafka, `policy-service`, `claims-service`, `reporting-service`.
- Each service has a `local` profile for H2 file mode and local URLs.
- Swagger UI at `http://localhost:<port>/swagger-ui.html`. OAuth2 login in Swagger uses the Keycloak URLs in `OpenApiConfig`.

## 15. Open questions and decisions

| # | Item | Impact |
|---|---|---|
| Q1 | How the threshold applies to non-INR markets | BR-4, section 5 |
| Q2 | Withdrawal from UNDER_REVIEW | BR-10, state machine |
| Q3 | Keycloak `market` attribute name and mapping | Section 6.1 |
| Q4 | Realm name and port | Sections 2, 6.1, 14 |
| Q5 | OTP delivery channel | Section 7.4 |
| Q6 | Internal SLA durations for queues | Section 8 of PRD. Not yet modelled. |
| D1 | Decided: local policy projection in claims-service, fed by `policies.changed` | Section 7.3 |
| D2 | Decided: policy-service owns `ClaimantContact` | Section 3.1 |
| D3 | Decided: assessment-service folded into claims-service for the first release | Section 2 |

## 16. Risks

- **Unverified versions.** Spring Boot 3.3.5 and springdoc 2.6.0 are not yet compiled. The first build will confirm compatibility.
- **H2 in production.** H2 is for local and test use. A production database decision is needed before deployment.
- **Projection lag.** A new or changed policy can take a few seconds to reach claims-service, so a claim filed right after policy creation may be rejected until the projection catches up (D1).
- **Multi-currency reporting.** Totals are per currency, so cross-market totals are not available without FX (out of scope).

## 17. Next steps

1. Review and resolve Q1 to Q5 and D1 to D3.
2. Install JDK 17 and Maven, then build `common`.
3. Build `policy-service` and `claims-service` with their migrations and tests.
4. Add the outbox relay and Kafka topics.
5. Build `reporting-service`.
6. Verify each endpoint in Swagger UI.

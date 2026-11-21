# Product Requirements Document: Claims Management Platform

| Field | Value |
|---|---|
| Product | Claims Management Platform (backend) |
| Owner | Chubb APAC claims team |
| Status | Draft for review |
| Version | 0.1 |
| Date | 2026-11-21 |

---

## 1. Purpose

Chubb APAC processes motor and property claims across six markets. Three problems exist today:

1. **Claimants lack status visibility.** Claims are submitted by phone or email, and claimants cannot see progress.
2. **Staff workload is fragmented.** Staff manage work across shared inboxes and spreadsheets, with no consolidated view of their queue.
3. **Managers lack a real-time view.** Managers cannot see outstanding claims or liability exposure as it changes.

This product is a set of Spring Boot microservices that expose REST APIs, documented and verified through Swagger (OpenAPI). It addresses all three problems through one claims lifecycle, role-based access, status notifications, and real-time reporting.

## 2. Scope

### 2.1 In scope

- REST APIs for claimants, claims staff, and managers.
- Claim intake online (self-service) and by phone or email (staff-assisted).
- Claim lifecycle, status history, and decisions.
- Reserve proposals and approvals.
- Policy management.
- Staff work queues and assignment.
- Manager dashboards for outstanding claims and liability exposure.
- Status notifications to claimants by email and SMS (SMS logged only for now).
- Authentication and authorization with OAuth2 and Keycloak.
- Event streaming with Apache Kafka.
- Local development without Docker.

### 2.2 Out of scope

- Frontend or UI of any kind. Swagger UI is the only verification tool.
- Currency conversion (FX). Each market reports in its registered currency.
- Market-specific claim types or rules. All six markets share one set for now.
- Real SMS provider integration. SMS uses a logging sender for now.
- Notifications in languages other than English.
- Payment disbursement. The system records settlement decisions only.
- Docker-based deployment.

## 3. Users and roles

Keycloak is the identity provider. Users are created in Keycloak and carry a role and, for staff, a `market` attribute.

| Role (Keycloak) | Who | Key capabilities |
|---|---|---|
| `CLAIMANT` | Policyholders and claimants | File claims against their own policies, view only their own claims, see status, withdraw while eligible. |
| `CLAIMS_STAFF` | Claims handlers | Create claims on behalf of phone or email submitters, review and assess claims, propose reserves and recommendations, manage their work queue, view policies. Limited to their own market. |
| `MANAGER` | Claims managers | Everything staff can do, plus: finalize settlement or rejection decisions, approve reserves, manage policies, view all markets, view exposure dashboards. |

### 3.1 Access rules

- **Claimants** see only claims where the token `sub` equals the claim's `ownerSubject`. Requests for claims they do not own return `404`, not `403`, so the existence of other claims is not revealed.
- **Claims staff** see claims in their own `market` only.
- **Managers** see all markets.
- **Policies:** Managers manage policies. Staff can read them. Claimants can read only policies they hold.

## 4. Business rules

| # | Rule | Source decision |
|---|---|---|
| BR-1 | A claim references exactly one policy. | Confirmed |
| BR-2 | A claimant may file a claim only against a policy that belongs to them. The system verifies ownership at submission. | Confirmed |
| BR-3 | Filing deadline is 30 days from the date of loss. Claims filed after 30 days are rejected at intake. | Confirmed |
| BR-4 | The threshold is ₹10 lakh. Claims at or above the threshold are flagged with `auditFlag = true`. How the threshold applies to non-INR markets is open; see Q1. | Confirmed (option C); Q1 open |
| BR-5 | Managers may approve any amount, including amounts above the threshold. The `auditFlag` makes these visible for audit. | Confirmed (option C) |
| BR-6 | Final settlement or rejection is decided by a Manager. Staff provide a recommendation. | Confirmed |
| BR-7 | Reserves are proposed by Claims Staff and approved by a Manager. | Confirmed |
| BR-8 | Each market's amounts are stored and reported in that market's registered currency. No FX conversion. Threshold comparison uses the amount as stated in the claim's currency; see Open Question Q1. | Confirmed in principle; Q1 open |
| BR-9 | All six markets share one set of claim types and rules. | Confirmed |
| BR-10 | A claimant may withdraw a claim only while it is `SUBMITTED`. | Proposed; Q2 open |

## 5. Claim intake

### 5.1 Online submission (claimant)

A claimant logs in through Keycloak, selects one of their policies, and submits a claim with date of loss, claim type (motor or property), and claimed amount. The system checks BR-2 and BR-3 before accepting the claim.

### 5.2 Phone or email submission (staff-assisted)

Claimants who report by phone or email do not have an account at the time of reporting. Staff record the claim against a `contactId` that identifies the claimant. This is option A.

- The claimant later claims ownership by verifying their policy number and a one-time password (OTP). Once verified, the claim's `ownerSubject` is set to the claimant's token `sub`, and the claimant can see the claim.
- Until verification, the claim is visible to staff only.

## 6. Claim lifecycle

### 6.1 Statuses

| Status | Meaning | Notifies claimant |
|---|---|---|
| `SUBMITTED` | Claim received and validated. | Yes |
| `UNDER_REVIEW` | Assigned to staff for assessment. | Yes |
| `PENDING_DECISION` | Staff recommendation recorded; awaiting Manager decision. | No |
| `APPROVED` | Manager approved the settlement. | Yes |
| `REJECTED` | Manager rejected the claim. | Yes |
| `SETTLED` | Settlement recorded as paid. | Yes |
| `WITHDRAWN` | Claimant withdrew the claim. | Yes |

Every status change is recorded in status history with the actor, the time, and the reason.

### 6.2 Transitions

- Staff move `SUBMITTED` → `UNDER_REVIEW`, and `UNDER_REVIEW` → `PENDING_DECISION` with a recommendation.
- Managers move `PENDING_DECISION` → `APPROVED` or `REJECTED`.
- `APPROVED` → `SETTLED` when settlement is recorded.
- Claimants move `SUBMITTED` → `WITHDRAWN` (see BR-10).

Exact transition rules are enforced in one state-machine class in claims-service, and invalid transitions return `422`.

## 7. Reserves

- Claims staff propose a reserve amount with a justification.
- A Manager approves or rejects the proposal.
- Approved reserves update the claim's outstanding exposure.
- Reserve changes do not notify claimants.

## 8. Staff workload

- Each staff member sees a queue of claims assigned to them in their market.
- Claims can be assigned and reassigned within the market by a Manager or the staff member themselves.
- The queue shows status, age, due date (based on BR-3 and internal SLAs), and claim type.
- Unassigned claims in a market are visible to all staff in that market.

## 9. Manager dashboards

- **Outstanding claims:** count and total claimed amount by market, status, and claim type, updated in near real time from Kafka events.
- **Liability exposure:** total approved reserves plus pending reserve proposals, by market and currency. Amounts are not converted across currencies.
- **Audit view:** claims with `auditFlag = true`.

Dashboards are served through REST endpoints. Data is built from events, so it can lag the source of truth by seconds.

## 10. Notifications

- Triggered by `ClaimStatusChanged` events, only when the status actually changes.
- Only the statuses marked "Yes" in section 6.1 notify the claimant.
- Channels: email (sent) and SMS (logged by a logging sender for now).
- Language: English only.
- Each claimant can enable or disable each channel.
- Messages contain claim reference and status, not personal data.
- Delivery is at-least-once. Notifications are deduplicated by event ID. Failed sends are retried, and messages that still fail go to a dead-letter topic.

## 11. Non-functional requirements

| Area | Requirement |
|---|---|
| Language and runtime | Java 17, Spring Boot 3.3.x |
| Build | Maven, multi-module |
| Persistence | H2 per service (file or in-memory for tests); schema managed by Flyway |
| Messaging | Apache Kafka (local install, KRaft mode, `localhost:9092`) |
| Identity | Keycloak (local install), OAuth2 resource server, JWT |
| API documentation | OpenAPI 3 via springdoc; Swagger UI for verification |
| Consistency | Transactional outbox in claims-service; consumers are idempotent |
| Security | Stateless; no PII in Kafka payloads; no secrets in source control |
| Observability | Structured logs with claim ID and event ID; health endpoints via Actuator |
| Testing | Unit tests for rules; `@DataJpaTest` with H2; `@EmbeddedKafka` for messaging; `spring-security-test` JWTs for roles |
| Local setup | No Docker |

## 12. Services

| Service | Responsibility |
|---|---|
| `common` | Security configuration, error handling, event envelope, OpenAPI configuration. |
| `policy-service` | Policies, ownership, claimant–policy links. |
| `claims-service` | Claims, lifecycle, status history, reserves, queues, outbox. |
| `notification-service` | Consumes status events, recipient preferences, email and SMS sending, dead-letter handling. |
| `assessment-service` | Folded into `claims-service` for the first release (recommendations). |
| `reporting-service` (planned) | Manager dashboards built from Kafka events. |

## 13. Key API areas

Final endpoint list is confirmed during detailed design. Expected areas:

- `/claims` (create, get, list, withdraw, assign, recommend, decide, settle)
- `/claims/{id}/reserves` (propose, approve)
- `/claims/{id}/history`
- `/policies` (manage, read)
- `/queues/me` (staff workload)
- `/dashboards/...` (manager views)
- `/notifications/claims/{claimId}`, `/recipients/me/preferences`, `/notifications/dead-letters`

## 14. Kafka topics (initial)

| Topic | Producer | Consumers | Key |
|---|---|---|---|
| `claims.status-changed` | claims-service | notification-service, reporting-service | `claimId` |
| `claims.reserve-changed` | claims-service | reporting-service | `claimId` |
| `claimants.contact-updated` | policy-service | notification-service | `ownerSubject` |
| `notifications.dead-letter` | notification-service | operators | `claimId` |

Event payloads carry IDs, status, market, and amounts. They do not carry names, email addresses, or phone numbers.

## 15. Success criteria

- Claimants can see their own claims and each status change, without contacting staff.
- Staff can see their queue in one place, for their market.
- Managers can see outstanding claims and exposure for all markets, with lag under 10 seconds from source change.
- Every rule in section 4 is enforced by an automated test.
- Every endpoint is documented and callable from Swagger UI.
- The full system runs locally with no Docker.

## 16. Milestones

1. **Foundation:** build tooling, `common` module, Keycloak realm, local Kafka.
2. **Policies and intake:** `policy-service`, claim submission, ownership checks, deadline and threshold rules.
3. **Lifecycle and reserves:** state machine, status history, outbox, reserve propose and approve.
4. **Queues and decisions:** staff queue, recommendation, Manager decision, settlement.
5. **Notifications:** `notification-service`, English email, logging SMS, dead-letter handling.
6. **Dashboards:** `reporting-service` for outstanding claims and exposure.
7. **Hardening:** tests, Swagger verification, documentation.

## 17. Open questions

| # | Question | Impact |
|---|---|---|
| Q1 | Should the ₹10 lakh threshold apply to the amount in each market's currency as stated, or to an equivalent value in INR? | BR-4, BR-8. Without FX, the threshold can only be applied per currency, so it needs a per-currency value. |
| Q2 | Can a claimant withdraw after `UNDER_REVIEW`, or only while `SUBMITTED`? | BR-10 |
| Q3 | What is the exact `market` attribute name and format in Keycloak? | Access rules (3.1) |
| Q4 | Which Keycloak realm name and port should be used? (Assumed: realm `claims`, port `8180`.) | Local setup |
| Q5 | What is the OTP delivery channel for phone-submitted claim verification? | Section 5.2 |
| Q6 | What are the internal SLA durations for queue due dates? | Section 8 |

## 18. Assumptions

- Keycloak and Kafka run as local installs. Docker is not used.
- Keycloak roles appear in `realm_access.roles` as `CLAIMANT`, `CLAIMS_STAFF`, and `MANAGER`.
- Each Claimant's identity in Keycloak is the same `sub` used as `ownerSubject`.
- Java 17 and Maven will be installed on the development machine before the build runs.

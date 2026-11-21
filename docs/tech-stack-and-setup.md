# Tech Stack and Setup

This guide lists the technology the claims platform uses and the steps to run it locally. Anything marked **external** (Kafka and Keycloak) is not provided by this repository, so it must be installed or started separately.

## 1. Tech stack

| Area | Technology | Version | Where it is used |
|---|---|---|---|
| Language | Java (JDK) | 17 | All modules (`<java.version>` in the root [pom.xml](C:/Users/shruthi.va/Documents/Chubb/backend-shruthi/pom.xml)) |
| Build | Apache Maven | 3.9+ recommended | Multi-module build |
| Framework | Spring Boot | 3.3.5 | Parent POM, all services |
| Health checks | Spring Boot Actuator | **not added** | Not in any POM yet |
| Web | Spring Web MVC | from Boot | REST controllers |
| Security | Spring Security, OAuth2 Resource Server | from Boot | JWT validation in [common](C:/Users/shruthi.va/Documents/Chubb/backend-shruthi/common) |
| Persistence | Spring Data JPA (Hibernate) | from Boot | Both services |
| Database | H2 (in-memory, PostgreSQL mode) | from Boot | One database per service |
| Migrations | Flyway | from Boot | Schema creation at startup |
| Messaging | Spring Kafka | from Boot | Outbox publishing and `policies.changed` listener |
| API docs | springdoc-openapi (Swagger UI) | 2.6.0 | Swagger UI on each service, `dev` profile only |
| Validation | Jakarta Bean Validation | from Boot | Request DTOs |
| Testing | JUnit 5, Mockito, AssertJ | from Boot | Unit and integration tests |
| Testing | spring-kafka-test (EmbeddedKafka) | from Boot | Kafka integration tests, no Docker needed |
| Testing | spring-security-test | from Boot | MockMvc JWT authentication |
| Identity provider | Keycloak | **external**, version to be fixed | Issues OAuth2 tokens |
| Messaging broker | Apache Kafka | **external**, version to be fixed | Events between services |

### Modules and ports

| Module | Purpose | Port |
|---|---|---|
| `common` | Shared security and error handling | none (library) |
| `policy-service` | Policies and claimant contacts | 8081 |
| `claims-service` | Claim intake, lifecycle, reserves, queries | 8082 |
| `notification-service` | Not yet built | 8083 (planned) |
| `reporting-service` | Not yet built | 8084 (planned) |

## 2. Prerequisites

Install these on the developer machine before starting.

1. **JDK 17.** Any 17.x distribution works (for example Eclipse Temurin). Verify with `java -version`.
2. **Apache Maven 3.9+.** Verify with `mvn -version`. The repository has no Maven wrapper, so Maven must be on the `PATH`.
3. **Git.** To clone the repository.
4. **Docker (optional).** Only needed if you choose to run Kafka and Keycloak in containers. The automated tests do not need Docker, because they use an embedded Kafka broker.

## 3. External services

### 3.1 Apache Kafka (external)

The services expect a broker at `localhost:9092` (set in both `application.yml` files under `spring.kafka.bootstrap-servers`).

Topics used by the platform:

| Topic | Producer | Consumer |
|---|---|---|
| `policies.changed` | policy-service (outbox) | claims-service (projection) |
| `claims.status-changed` | claims-service (outbox) | notification-service (planned) |
| `claims.reserve-changed` | claims-service (outbox) | reporting-service (planned) |
| `claimants.contact-updated` | policy-service (outbox) | notification-service (planned) |

The services create topics automatically when first used. For production, create them explicitly with the partition and retention settings agreed in the design.

### 3.2 Keycloak (external)

The services expect Keycloak at `http://localhost:8180`, with a realm named `claims` (set as `spring.security.oauth2.resourceserver.jwt.issuer-uri` in both services).

Configure the realm as follows. The repository does not contain the realm export, so this is manual until one is added.

1. **Realm:** `claims`.
2. **Realm roles:** `CLAIMANT`, `CLAIMS_STAFF`, `MANAGER`. The services read them from `realm_access.roles` and map them to `ROLE_*` authorities.
3. **User attribute / token claim:** a `market` claim on each user (for example `IN`, `HK`). Staff access is limited to this market. Managers see all markets.
4. **Client for Swagger:** `claims-swagger` (public client, authorization code with PKCE). Swagger UI uses this client ID (set under `springdoc.swagger-ui.oauth.client-id`).
5. **Users:** create users in Keycloak and assign one role each. Claimants keep their own Keycloak subject (`sub`), which the services store as `ownerSubject`.

## 4. Configuration reference

Settings that matter when running locally. All values are in each service's `src/main/resources/application.yml`.

| Setting | policy-service | claims-service |
|---|---|---|
| Port | 8081 | 8082 |
| Database URL | `jdbc:h2:mem:policydb` | `jdbc:h2:mem:claimsdb` |
| Database user / password | `sa` / (empty) | `sa` / (empty) |
| Kafka bootstrap | `localhost:9092` | `localhost:9092` |
| Token issuer | `http://localhost:8180/realms/claims` | same |
| Outbox relay interval | 500 ms | 500 ms |

Claims-service also reads:

- `claims.filing-window-days`: 30
- `claims.audit-thresholds.INR`: 1,000,000 (INR only; other currencies fail closed and set the audit flag)
- `claims.otp.stub-code`: `123456`. **This stub must be replaced before any production use.**

The H2 databases are in memory, so data is lost when a service stops. Flyway recreates the schema on each start.

## 5. Build

From the repository root:

```powershell
mvn clean install
```

This builds `common`, then `policy-service` and `claims-service`, and runs all unit and integration tests. The first build downloads the Spring Boot dependencies, which takes several minutes.

To build one module:

```powershell
mvn -pl claims-service -am clean install
```

## 6. Run the services

Start Kafka and Keycloak first (section 3). Then, in separate terminals:

```powershell
mvn -pl policy-service spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl claims-service spring-boot:run -Dspring-boot.run.profiles=dev
```

Policy-service must be running before claims-service receives policy events, but the claims service starts without it.

## 7. Verify

Swagger UI and the OpenAPI document are **disabled by default**. They are enabled only when the `dev` profile is active (the `-Dspring-boot.run.profiles=dev` flag above, or `SPRING_PROFILES_ACTIVE=dev`). Without it, `/swagger-ui.html` and `/v3/api-docs` return 404. The `prod` profile (`application-prod.yml`) turns them off explicitly, so they stay off even if `dev` is also active. Do not enable the `dev` profile in shared or production environments.

| Check | URL (dev profile only) |
|---|---|
| policy-service Swagger UI | http://localhost:8081/swagger-ui.html |
| claims-service Swagger UI | http://localhost:8082/swagger-ui.html |
| OpenAPI JSON (claims) | http://localhost:8082/v3/api-docs |

Use the **Authorize** button in Swagger UI to sign in through Keycloak. Each API call needs a valid bearer token.

The H2 console is not enabled in the configuration files.

## 8. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `java` or `mvn` not found | JDK or Maven not on `PATH` | Install per section 2 and reopen the terminal |
| Service fails at startup fetching `.well-known/openid-configuration` | Keycloak not running or realm `claims` missing | Start Keycloak and create the realm (section 3.2) |
| 401 on every call from Swagger | Token missing or issuer mismatch | Sign in through Swagger UI; check the issuer URI matches the Keycloak realm URL |
| 403 for an expected action | User role wrong or missing | Check the realm role assignment in Keycloak |
| Staff sees no claims | `market` claim missing or different from the policy market | Set the `market` claim on the user |
| Events not reaching claims-service | Kafka not running, or policy-service outbox not relaying | Start Kafka; check the policy-service logs for relay errors |
| Port already in use | Another process on 8081 or 8082 | Stop the other process, or change `server.port` |

## 9. Not yet in the repository

These are planned, so they are not needed to run the current services:

- Docker Compose file for Kafka and Keycloak
- Keycloak realm export
- notification-service and reporting-service
- CI pipeline
- Production OTP provider (replacing the stub)

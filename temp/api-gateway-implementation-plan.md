# API Gateway Implementation Plan

Status: **planning only — no gateway code or runtime configuration has been implemented yet.**

## 1. Goal

Add a Spring Cloud Gateway that becomes the single public HTTP entry point for the library system. The gateway will discover healthy backend instances through the existing Consul registry and route requests to the correct service without hard-coded container addresses.

The first version will deliberately have **no authentication or authorization**. Keycloak, JWT validation, Spring Security, roles, scopes, LDAP, token relay, and login flows from `temp/key-cloak.md` are postponed to a later phase.

The intended request flow is:

```text
Browser / API client
        |
        | http://localhost:8000/api/...
        v
   API Gateway  ----------------->  Consul
        |                         find a healthy instance
        |
        +---- lb://catalog-service -----> Catalog service
        +---- lb://inventory-service ---> Inventory service
        +---- lb://membership-service --> Membership service
        +---- lb://borrowing-service ---> Borrowing service
```

`lb://` is important: the route names a logical service, while Spring Cloud LoadBalancer and Consul resolve its current network location. The gateway therefore does not need to know that Catalog currently runs in a container named `catalog-service-app`, for example.

## 2. Current repository facts that shape the design

- The four services already register with Consul as `catalog-service`, `inventory-service`, `membership-service`, and `borrowing-service`.
- Their APIs use distinct path groups, so they can share one public `/api/...` namespace without rewriting paths.
- The services currently use Spring Boot `4.1.1`, Spring Cloud `2025.1.2`, Java 17, Kotlin, and Maven. The gateway should use the same baseline to avoid introducing another compatibility matrix.
- The root `docker-compose.yml` owns Consul and creates `shared_net`. Each backend compose project joins that network.
- The frontend is currently the untouched Vite starter and does not make API calls yet. Its normal development origin is expected to be `http://localhost:5173`.
- Backend ports `8087`–`8090` are still published to the host. They should stay published in this first gateway phase because they are useful for direct comparison and debugging; making the gateway the only exposed backend port can be a later hardening step.
- Port `8000` is currently unused and matches the gateway port used in the reference material.

## 3. Key design decisions

### 3.1 Create a separate reactive service

Create a sibling project named `api-gateway/`, implemented in Kotlin like the other services. It will be an independent Spring Boot Maven application rather than a module nested inside one business service.

Use these main dependencies:

- `spring-cloud-starter-gateway-server-webflux` — the current Spring Cloud Gateway WebFlux starter for the repository's Spring generation.
- `spring-cloud-starter-consul-discovery` — service registration and discovery.
- `spring-cloud-starter-loadbalancer` — resolves `lb://...` destinations to discovered instances.
- `spring-boot-starter-actuator` — health, information, and read-only route diagnostics.
- Kotlin runtime/reflection and the normal Spring Boot/Kotlin test dependencies.

Do **not** add Spring MVC, database/JPA, Kafka, Axon, OpenFeign, Spring Security, OAuth2 client, or OAuth2 resource-server dependencies. The gateway is a reactive infrastructure service and does not own business data or business logic.

### 3.2 Use explicit public routes backed by discovery

Define routes under `spring.cloud.gateway.server.webflux.routes`. Each route will target a Consul service through an `lb://` URI.

Do not enable the automatic DiscoveryClient route locator in this phase. Automatic routes would expose implementation-shaped paths such as `/catalog-service/**`. Explicit routes provide a stable client API and prevent every newly registered internal service from becoming public accidentally.

No `StripPrefix` or `RewritePath` filter is needed because the downstream controllers already expect the same `/api/...` paths that clients will call.

| Gateway path predicate | Consul destination | Example public request |
|---|---|---|
| `/api/books/**`, `/api/categories/**` | `lb://catalog-service` | `GET /api/books/all` |
| `/api/libraries/**`, `/api/stock/**`, `/api/transfers/**` | `lb://inventory-service` | `GET /api/libraries/all` |
| `/api/members/**` | `lb://membership-service` | `GET /api/members/all` |
| `/api/loans/**`, `/api/fees/**`, `/api/payments/**`, `/api/borrowing-bans/**` | `lb://borrowing-service` | `GET /api/loans/all` |

The gateway should preserve the HTTP method, request body, query string, and normal request headers. It should pass the downstream status, headers, and response body back to the caller.

### 3.3 Keep cross-cutting behavior deliberately small

- Configure global CORS at the gateway for the Vite development origin (`http://localhost:5173` by default), with the origin externalizable for other environments. Allow the HTTP methods and headers used by the APIs. Do not use a wildcard origin together with credentials.
- Do not add retries to mutating requests. An automatic retry of a `POST`, `PUT`, or `DELETE` can duplicate a command. Resilience policy can be designed later per route and HTTP method.
- Do not add circuit-breaker fallbacks that fabricate business responses. If Consul has no healthy instance or a service is unavailable, return the gateway's normal upstream failure response.
- Do not add custom header filters unless testing reveals a concrete need. Spring Cloud Gateway already supplies standard forwarding behavior.

### 3.4 Expose only useful operational endpoints

- Register the gateway itself in Consul as `api-gateway`.
- Provide `/actuator/health` for Docker and Consul health checks.
- Expose `/actuator/info` and the gateway route actuator endpoint.
- Configure the gateway actuator endpoint as **read-only**, so it can display resolved routes but cannot create, delete, or refresh routes over HTTP.
- Keep these endpoints available for local development. Their exposure must be revisited when the later security phase is implemented.

## 4. Planned file changes

### New `api-gateway/` project

1. `api-gateway/pom.xml`
   - Match Java 17, Spring Boot `4.1.1`, Kotlin `2.3.21`, and Spring Cloud `2025.1.2` already used by the services.
   - Import the Spring Cloud BOM and add only the reactive gateway/discovery/actuator dependencies described above.

2. `api-gateway/mvnw`, `api-gateway/mvnw.cmd`, and `api-gateway/.mvn/wrapper/...`
   - Include the Maven Wrapper so the gateway can be built the same way as every existing service.

3. `api-gateway/src/main/kotlin/com/apigateway/ApiGatewayApplication.kt`
   - Add the minimal Spring Boot entry point.
   - Avoid controllers and business services; routing belongs in configuration.

4. `api-gateway/src/main/resources/application.yml`
   - Set `spring.application.name=api-gateway` and port `8000`.
   - Point Consul to environment-controlled host/port values, defaulting to `localhost` for a host-run JVM and overridden to `consul` in Compose.
   - Register a unique gateway instance and configure `/actuator/health` as its Consul check.
   - Define the four explicit route groups shown above with `lb://` targets.
   - Configure externalizable development CORS.
   - expose health/info plus read-only gateway diagnostics.

5. `api-gateway/Dockerfile`
   - Follow the repository's multi-stage Maven build pattern.
   - Produce a small Java 17 runtime image, expose port `8000`, and add a usable container health check.

6. `api-gateway/src/test/...`
   - Add an application-context smoke test with Consul registration disabled.
   - Add route-definition assertions so every public path group points at the intended logical service and accidental auto-discovery routes are absent.
   - Add focused proxy tests using test-only discovered service instances and local stub HTTP servers. Verify representative Catalog, Inventory, Membership, and Borrowing requests, including preservation of query parameters, bodies, and response status.
   - Verify an unmatched path returns `404` and an unavailable discovered service produces a gateway/upstream failure rather than a false successful response.

### Existing repository files

7. Root `docker-compose.yml`
   - Add an `api-gateway` service built from `./api-gateway`.
   - Publish `8000:8000`, join `shared_net`, configure Consul as `consul:8500`, wait for Consul's health check, and add a gateway container health check.
   - Do not add dependencies on the four backend containers because they belong to separate Compose projects and may start or scale independently. Consul discovery allows them to appear after the gateway starts.

8. `INTEGRATION.md`
   - Update startup instructions to explain that the root stack now includes the gateway.
   - Document the public route table and example `curl` commands.
   - Explain how to inspect Consul and `/actuator/gateway/routes` when a request returns `503` or a route is missing.
   - Keep direct backend URLs documented as development diagnostics, while identifying port `8000` as the client-facing entry point.

9. `.gitignore`
   - Generalize the backend build-output rules if necessary so `api-gateway/target/` and the existing services' `target/` directories are ignored consistently. Do not disturb unrelated ignore rules.

No business-service controller, repository, domain model, Kafka contract, Pact contract, or database configuration should need to change for this phase.

## 5. Implementation sequence

1. Scaffold the gateway Maven/Kotlin project with version-compatible reactive dependencies.
2. Add the minimal application entry point and confirm a test-profile context starts without Consul.
3. Configure Consul registration, explicit route predicates, load-balanced destinations, CORS, and actuator access.
4. Add automated route and proxy tests before container integration.
5. Add the gateway Dockerfile and root Compose service.
6. Build and run the root infrastructure plus the four service Compose projects.
7. Check Consul registrations and exercise at least one endpoint from every route group through port `8000`.
8. Update integration documentation with the verified commands and troubleshooting observations.

## 6. Verification checklist

Automated checks:

```text
api-gateway Maven test suite passes
gateway application context starts with discovery disabled in tests
configured route table contains exactly the intended public routes
representative requests are proxied without path/body/query corruption
unmatched paths are not exposed
```

Container/integration checks:

```text
docker compose config succeeds
api-gateway container becomes healthy
Consul reports api-gateway and all running backend services healthy
GET http://localhost:8000/actuator/health returns 200
GET http://localhost:8000/actuator/gateway/routes lists read-only explicit routes
one Catalog request through :8000 reaches catalog-service
one Inventory request through :8000 reaches inventory-service
one Membership request through :8000 reaches membership-service
one Borrowing request through :8000 reaches borrowing-service
an unknown /api path returns 404
stopping one backend causes only that backend's routes to fail; the gateway remains healthy
```

## 7. Acceptance criteria

The phase is complete when:

- clients can use `http://localhost:8000` as the single base URL for every existing REST API;
- gateway destinations are resolved from Consul service IDs, not hard-coded backend hostnames or host ports;
- public paths remain the same as the current controller paths;
- only explicitly listed API groups are routable;
- health and route diagnostics are available and route mutation is disabled;
- browser calls from the configured frontend development origin pass CORS preflight;
- the gateway is buildable/testable independently and starts as part of the root Compose stack;
- all automated and smoke checks above pass; and
- no Keycloak, JWT, LDAP, or authorization behavior is introduced.

## 8. Deferred work

The following items are intentionally outside this approval:

- Keycloak and LDAP containers or configuration.
- Spring Security and OAuth2 resource-server dependencies.
- JWT signature, issuer, audience, role, or scope validation.
- Token relay or per-route authorization rules.
- Rate limiting, Redis, circuit breakers, retries, or fallback payloads.
- Aggregated OpenAPI/Swagger UI.
- Removing direct host-port access to backend services.
- TLS, production DNS, and deployment outside local Docker Compose.

When authentication is approved later, it can be added as a gateway filter/security layer without changing the public route design in this plan.

## 9. Main risks and mitigations

| Risk | Mitigation in this plan |
|---|---|
| Copying dependency names or properties from an older gateway tutorial | Use the current WebFlux starter and `spring.cloud.gateway.server.webflux.*` namespace that match Spring Cloud Gateway 5.x. |
| Accidentally exposing every Consul service | Keep automatic discovery routes disabled and declare an allow-list of path predicates. |
| Routing to an unhealthy or changed container address | Route to `lb://<service-id>` and let Consul supply healthy instances. |
| Duplicate side effects caused by retries | Add no blanket retry filter, especially for command endpoints. |
| CORS duplicated inconsistently across services | Own browser-edge CORS in the gateway and make allowed origins configurable. |
| Diagnostics becoming a write/control surface | Set the gateway actuator endpoint to read-only; reassess access during the security phase. |
| Gateway starts before business services | Treat this as normal; discovery updates as services register. Test that the gateway remains healthy. |

## 10. Reference notes

The architecture idea comes from `temp/key-cloak.md`, with all identity-related portions excluded. The plan also accounts for the current official Spring Cloud Gateway behavior:

- The current reactive starter is `spring-cloud-starter-gateway-server-webflux`: <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html>
- Current YAML route configuration is nested below `spring.cloud.gateway.server.webflux.routes`: <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/configuration.html>
- Load-balanced discovery routes require Spring Cloud LoadBalancer: <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/the-discoveryclient-route-definition-locator.html>
- Gateway actuator access can be set to read-only: <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/actuator-api.html>
- Consul discovery is enabled with `spring-cloud-starter-consul-discovery`: <https://docs.spring.io/spring-cloud-consul/reference/discovery.html>


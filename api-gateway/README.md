# API Gateway

Spring Cloud Gateway is the public HTTP entry point for the library system. It preserves the existing `/api/...` paths and resolves backend instances through Consul.

## Routes

| Paths | Destination |
|---|---|
| `/api/books/**`, `/api/categories/**` | `catalog-service` |
| `/api/libraries/**`, `/api/stock/**`, `/api/transfers/**` | `inventory-service` |
| `/api/members/**` | `membership-service` |
| `/api/loans/**`, `/api/fees/**`, `/api/payments/**`, `/api/borrowing-bans/**` | `borrowing-service` |

All routes through port `8000`, except `/actuator/health`, require a valid Keycloak access token. Direct service ports remain unchanged. See [KEYCLOAK-INSTRUCTIONS.md](KEYCLOAK-INSTRUCTIONS.md) for startup, user and role administration, token acquisition, and troubleshooting.

## Run with Docker

Start the repository's root Compose stack first so Consul and `shared_net` exist. Start any backend services you need, then run:

```shell
docker compose up -d --build
```

The gateway is available at `http://localhost:8000`, and Keycloak is available at `http://localhost:8180`. The gateway health endpoint is public at `/actuator/health`; its read-only route list at `/actuator/gateway/routes` requires a token.

A minimal unauthenticated Membership request now returns `401`:

```shell
curl http://localhost:8000/api/members/all
```

Follow [KEYCLOAK-INSTRUCTIONS.md](KEYCLOAK-INSTRUCTIONS.md) to obtain a token and make an authenticated request.

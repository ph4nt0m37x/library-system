# API Gateway

Spring Cloud Gateway is the public HTTP entry point for the library system. It preserves the existing `/api/...` paths and resolves backend instances through Consul.

## Routes

| Paths | Destination |
|---|---|
| `/api/books/**`, `/api/categories/**` | `catalog-service` |
| `/api/libraries/**`, `/api/stock/**`, `/api/transfers/**` | `inventory-service` |
| `/api/members/**` | `membership-service` |
| `/api/loans/**`, `/api/fees/**`, `/api/payments/**`, `/api/borrowing-bans/**` | `borrowing-service` |

Authentication and authorization are intentionally not part of this version.

## Run with Docker

Start the repository's root Compose stack first so Consul and `shared_net` exist. Start any backend services you need, then run:

```shell
docker compose up -d --build
```

The gateway is available at `http://localhost:8000`. Its health endpoint is `/actuator/health`, and its read-only route list is `/actuator/gateway/routes`.

A minimal membership route check is:

```shell
curl http://localhost:8000/api/members/all
```

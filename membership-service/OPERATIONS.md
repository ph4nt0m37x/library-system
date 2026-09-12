# Membership Service Operations

## Canonical container values

- Membership HTTP port: `8087` (`8087:8087`)
- MySQL host and container port: `3307` (`3307:3307`)
- Docker datasource URL: `jdbc:mysql://mysqldb:3307/membership_service_db`
- Consul: `consul:8500`

These values match `docker-compose.yml` and `.env.example`.

## Startup order

1. Start the root Compose stack so `shared_net`, Consul, and Kafka are available.
2. Start the Membership Compose stack. Its application waits for the Membership MySQL health check.
3. Allow Membership to register with Consul, then verify `http://localhost:8087/actuator/health`.

The application Docker health check uses the actuator endpoint. The Compose file remains the source of the MySQL and application port mappings.

## Restart policy

The existing Compose file already restarts MySQL. To apply the same policy to the Membership application container without changing that file, run this once after the container is created:

```text
docker update --restart unless-stopped membership-service_app
```

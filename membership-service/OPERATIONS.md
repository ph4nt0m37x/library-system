# Membership Service Operations

## Canonical container values

- Membership HTTP port: `8087` (`8087:8087`)
- MySQL host and container port: `3306` (`3307:3306` from the host)
- Docker datasource URL: `jdbc:mysql://mysqldb:3306/membership_service_db`
- Consul: `consul:8500`

These values match `docker-compose.yml` and `.env.example`.

## Startup order

1. Start the root Compose stack so `shared_net` and Consul are available. Membership deliberately has no Kafka dependency because no service consumes Membership events.
2. Start the Membership Compose stack. Its application waits for the Membership MySQL health check.
3. Allow Membership to register with Consul, then verify `http://localhost:8087/actuator/health`.

The application Docker health check uses the actuator endpoint. The Compose file remains the source of the MySQL and application port mappings.

## Restart and health policy

Both MySQL and the Membership application have restart policies. Docker checks the application's actuator endpoint; Consul performs the same process-health check after registration. Start the shared infrastructure first because Compose cannot express `depends_on` across separate Compose projects.

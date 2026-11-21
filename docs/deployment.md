# Deployment

## Database
The application uses MySQL (`quarkus-jdbc-mysql`) in the container `blog-mysql` from `docker-compose.yml`,
the data lives in the named volume `blog-data`. The dev mode and the tests reach it via the port forward
`localhost:3306`, the backend container via `blog-mysql:3306` in the network `blog-nw`. The password of
`dbuser` comes from `QUARKUS_DATASOURCE_PASSWORD` in the `.env`.

The schema is managed by Flyway: on start (`quarkus.flyway.migrate-at-start=true`) it applies the scripts
in `src/main/resources/db/migration` that are not yet in the table `flyway_schema_history`. Hibernate only
checks that the entities match (`schema-management.strategy=validate`). A schema change therefore needs a
new script `V<n>__<description>.sql`, never an edit of an applied one. `DataInitialization` inserts the
sample data only into an empty database, so data survives restarts of the backend and the database.

The tests share the same database and empty it before every run (`%test.quarkus.flyway.clean-at-start`).

---
## Production build with Docker
In production mode the application runs as a container next to Keycloak, both in the Docker network `blog-nw`.
Quarkus runs with the `prod` profile there: no Dev Services and no hot reload.

1. **Build the JAR and the image.** The extension `quarkus-container-image-docker` together with
   `quarkus.container-image.build=true` builds the image `ghcr.io/michael-hftm/blog-backend:latest` during `package`
   with `src/main/docker/Dockerfile.jvm`:

   ```shell script
   ./mvnw package
   ```

   The first build downloads the base image and therefore takes a while without showing any progress.

2. **Provide the secrets.** `docker-compose` reads the `.env` in the project folder (see
   [Getting started](../README.md#getting-started)) and hands `QUARKUS_OIDC_CREDENTIALS_SECRET` to the
   backend container. The image itself contains no secret. Without the variables `docker-compose` refuses to start.

3. **Start Keycloak and the backend:**

   ```shell script
   docker-compose up -d
   ```

   `blog-backend` waits until the Keycloak healthcheck succeeds (`depends_on` with `service_healthy`).
   Inside the network the backend reaches Keycloak at `http://keycloak:8180`, the same URL as in
   `quarkus.oidc.auth-server-url`, so no extra configuration is needed.

The API runs on <http://localhost:8080>, the Swagger UI is included in production as well
(`quarkus.swagger-ui.always-include=true`). After a code change, run `./mvnw package` again and recreate
the container with `docker-compose up -d blog-backend`.

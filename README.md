# Blog Project DS1 


![Quarkus](https://img.shields.io/badge/quarkus-%234794EB.svg?style=for-the-badge&logo=quarkus&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/apachemaven-C71A36.svg?style=for-the-badge&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

This is a project for the class 'Distributed Systems 1' at HFTM Grenchen.

---

The currently implemented features are:
- Docker-Compose for local database setup
- Some blog entities with corresponding repositories, services and tests 
  - DataInitialization.java inserts some initial data into the database
- Basic CRUD operations for the blog entities
- REST API for the blog entities (see [REST API](#rest-api) below and the [Swagger UI](http://localhost:8080/q/swagger-ui))
  - Search and paging with meta data on the list endpoint
  - Bean Validation and a uniform JSON error format
  - Own request/response DTOs instead of the entity (see [DTOs and mapping](#dtos-and-mapping))
  - CORS enabled for browser clients
- Automated tests: service tests plus an HTTP level test of the API with REST Assured


---
# Getting started

Run these steps from the root folder to test the auth setup:

1. **Add the hosts entry** once, see [Keycloak setup](#setup).
2. **Start the database and Keycloak:**

   ```shell script
   docker-compose up -d
   ```

3. **Start the application** once Keycloak has imported the realm, which takes a moment on the first start:

   ```shell script
   ./mvnw quarkus:dev
   ```

The API now runs on <http://localhost:8080>. The test users are listed in
[Realm contents](#realm-contents), example calls are in [Calling the API](#calling-the-api).

---
# REST API

All endpoints live under `/blogs`, consume and produce `application/json`, and are implemented in `BlogResource`.<br>
Every method returns a Jakarta REST `Response`, so status codes and headers are set explicitly.<br>
An interactive version of this overview is available in the [Swagger UI](http://localhost:8080/q/swagger-ui).

| Method   | Path          | Success            | Description                                              |
|----------|---------------|--------------------|----------------------------------------------------------|
| `GET`    | `/blogs`      | `200`              | List blogs, paged and optionally filtered by a search term |
| `GET`    | `/blogs/{id}` | `200`              | A single blog                                             |
| `POST`   | `/blogs`      | `201` + `Location` | Create a blog                                        |
| `PATCH`  | `/blogs/{id}` | `200`              | Partial update; only the fields present in the body change |
| `DELETE` | `/blogs/{id}` | `204`              | Delete a blog and its comments                           |

## DTOs and mapping

The REST layer never hands out the JPA entity. Each endpoint has its own record in
`ch.diamondh3art.blog.boundry.dto`, so the persistence model can change without breaking clients
and no field leaks into the API by accident:

| DTO             | Used by                     | Fields                      |
|-----------------|-----------------------------|-----------------------------|
| `NewBlogDto`    | Request body of `POST`      | `title`, `content` (both required) |
| `UpdateBlogDto` | Request body of `PATCH`     | `title`, `content` (both optional) |
| `BlogDto`       | Response of every blog endpoint | `id`, `title`, `content` |

What is released: `BlogDto` exposes `id`, `title` and `content`. The entity's `author` stays internal,
and comments are not part of it, the relation is owned by `Comment` and there is no REST endpoint for
comments yet. The request DTOs drop `id` and `author` on purpose, both are set by the server, so a
client cannot set or overwrite them.

The mapping is written by hand, one static factory per direction, no mapping framework:

- `NewBlogDto.toBlog()` and `UpdateBlogDto.toBlogPatch()` turn a request body into a `Blog` that is
  passed on to `BlogService`. The patch carrier is an unmanaged `Blog` whose null fields mean
  "not sent, keep the stored value", the merge itself happens in `BlogService.updateBlog`.
- `BlogDto.from(Blog)` maps a stored blog to the response, and the `from(List<Blog>)` overload does
  the same for the paged list, so `GET /blogs` re-wraps the `PagedResponse` with mapped items and
  the unchanged `meta`.

Validation stays on the entity (`@NotBlank`): `POST` validates the mapped `Blog`, `PATCH` validates
the merged one, so both paths share one set of rules.

## Listing, searching and paging

`GET /blogs` accepts three optional query parameters:

| Parameter      | Default | Meaning                                                              |
|----------------|---------|----------------------------------------------------------------------|
| `searchString` | –       | Case-insensitive substring match on title **or** content              |
| `page`         | `0`     | Zero-based page index                                                 |
| `size`         | `20`    | Records per page, between 1 and 100                                   |

The records are wrapped in a `data` property so that the response can carry meta data alongside
them, without it a client could not tell how many records exist in total:

```http
GET /blogs?searchString=jedi&page=1&size=5
```
```json
{
  "data": [ { "id": 6, "title": "...", "content": "..." } ],
  "meta": { "totalCount": 42, "pageCount": 9, "page": 1, "size": 5, "count": 5 }
}
```

`totalCount` and `pageCount` refer to the *filtered* result, so they stay correct when
`searchString` is used.<br>
A page beyond the last one returns an empty `data` array rather than a 404. Records are always ordered by `id`.

## Creating a blog

`POST /blogs` returns the created record and, per REST convention, its URI in the `Location`
header, the id is generated by the server, so the client cannot know it beforehand:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/blogs/7
```

## Deleting a blog

`DELETE /blogs/{id}` removes the blog and its comments. Only `admin` may call it, see
[Authorization concept](#authorization-concept) below; this replaces the shared-secret mechanism
that used to guard this endpoint.

## Validation

`title` and `content` must not be blank (`@NotBlank` on the `Blog` entity).<br>
`POST` validates the request body via `@Valid`.<br>
`PATCH` validates the *merged* entity in `BlogService`, so a partial body like `{"content":"..."}` stays valid while a blank title is still rejected.<br>
An invalid update
is rolled back, the stored blog keeps its previous values.

## Error responses

Every error uses the same shape, so a client needs only one code path.<br>
The message is repeated in an `info` header. `violations` is only present for validation errors:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "violations": [ { "field": "title", "message": "Titel darf nicht leer sein." } ]
}
```

| Status | When                                                                      |
|--------|---------------------------------------------------------------------------|
| `400`  | Failed validation, missing request body, `page`/`size` out of range, malformed JSON |
| `401`  | Write endpoint called without a login                                      |
| `403`  | Logged in, but the required role is missing or the blog belongs to someone else |
| `404`  | No blog with the given id                                                  |
| `500`  | Unexpected server error (logged, no internal details in the body)          |

Errors are produced by `ResponseException`, which carries the whole response, and by three mappers:<br>
`ValidationExceptionMapper` for `@Valid` failures, `SecurityExceptionMappers` for failed role checks
and `RestExceptionMapper` as a safety net for everything raised outside our own code.<br>
One exception: an invalid or expired token is rejected by Quarkus before the REST layer, so that
`401` has an empty body, and it also hits the public `GET` endpoints.

## CORS

Browser clients are served from a different origin than the API, so CORS is enabled in
`application.properties`:

```properties
quarkus.http.cors.enabled=true
quarkus.http.cors.origins=*
```

Quarkus then answers the preflight `OPTIONS` request and adds the `Access-Control-Allow-Origin`
header to the responses. `*` is fine here because the API runs locally only and there are no
cookie-based sessions yet; a deployment would replace it with the concrete frontend origin, and
once OIDC tokens are sent as a `Bearer` header rather than a cookie, no
`access-control-allow-credentials` is needed either.

## Security (OWASP API Top 10 2023)

The relevant risks for this API and where the project stands. Reading is anonymous by design, write
access is restricted by role and, for `PATCH`, by blog ownership:

| Risk | State | Notes |
|------|-------|-------|
| **API1 - Broken Object Level Authorization** | covered | `POST` stores the logged-in user as the blog's `author`, and `PATCH` rejects an `author` who does not own the blog with `403` (see [Authorization concept](#authorization-concept)). Reading stays public on purpose, deleting is `admin`-only. |
| **API2 - Broken Authentication** | covered | Write endpoints require a Keycloak/OIDC bearer token. |
| **API3 - Broken Object Property Level Authorization** | covered | The DTOs are the defence here (see [DTOs and mapping](#dtos-and-mapping)). `NewBlogDto`/`UpdateBlogDto` have no `id`, so mass assignment cannot overwrite the key, and `BlogDto` is an explicit whitelist, so a new entity field is not exposed by accident. |
| **API4 - Unrestricted Resource Consumption** | partly | `size` is rejected outside 1..100, so a single request cannot pull the whole table, and Quarkus caps the request body (`quarkus.http.limits.max-body-size`, 10M by default). Missing is any rate limiting - a loop of `POST /blogs` fills the database unchecked. |
| **API5 - Broken Function Level Authorization** | covered | Create, patch and delete are restricted with `@RolesAllowed` per the [authorization concept](#authorization-concept) below. |
| **API6 - Unrestricted Access to Sensitive Business Flows** | partly | Creating blogs is exactly the flow worth automating for spam, and nothing (captcha, quota, account) stands in the way. Same root cause as API4. |
| **API7 - Server Side Request Forgery** | not applicable | The API never fetches a remote resource from user input. |
| **API8 - Security Misconfiguration** | partly | Good: `RestExceptionMapper` logs the exception and answers with a generic `500`, so no stack traces leak. Open: `quarkus.http.cors.origins=*`, DB credentials sit in `application.properties` instead of env vars, `schema-management.strategy=drop-and-create` drops the schema on every start, and the API is served over plain HTTP. Acceptable locally, none of it for a deployment. |
| **API9 - Improper Inventory Management** | covered (small scale) | Five endpoints, all annotated with MicroProfile OpenAPI and documented above and in `/q/openapi`. The Swagger UI is a dev-mode-only endpoint by default, so it is not an exposed debug surface in a build. The paths carry no version yet (`/blogs`, not `/v1/blogs`). |
| **API10 - Unsafe Consumption of APIs** | not applicable | No third-party API is consumed; the only downstream is the database, and queries go through Panache with bound parameters (`BlogRepository.search`), so the search term cannot be injected. |

Short version: input handling, data exposure, role and ownership checks are in decent shape,
per-caller quotas are the open construction site.

## Authorization concept

This is the concept that closes the gaps from API1/API2/API5 above. It is enforced with
Keycloak/OIDC, `@RolesAllowed` on `BlogResource` and an ownership check on `PATCH`.

### Public endpoints (no login required)

Reading is anonymous, since a blog is meant to be read by anyone:

| Method | Path          | Public? | Notes                                   |
|--------|---------------|---------|------------------------------------------|
| `GET`  | `/blogs`      | yes     | List/search all blogs                   |
| `GET`  | `/blogs/{id}` | yes     | Read a single blog                      |

### Roles

Three roles cover the domain, ordered from least to most permissions:

| Role     | Meaning                                                                 |
|----------|--------------------------------------------------------------------------|
| `reader` | Default role for every authenticated user. No write access, only used so that future features (e.g. commenting, favorites) have an identity to attach to. Not required for the read endpoints above, which stay public even without this role. |
| `author` | Can create blogs and edit the blogs they created themselves. Cannot edit or delete other authors' blogs, and cannot delete any blog. |
| `admin`  | Full access: create/edit/delete any blog regardless of owner. Replaces the shared-secret check that used to guard `DELETE`. |

Roles are Keycloak realm roles assigned per user. The hierarchy is not checked in code but
modelled as composite roles in `src/blog-realm.json`: `admin` is composite over `author`, so it implies the
permissions of `author` without duplicating checks in code, and `reader` is a composite member of
`default-roles-blog`, Keycloak's own default role, so every newly registered user gets `reader`
automatically, no manual role assignment needed.

### Access rules

| Method   | Path          | Required role          | Rule                                                               |
|----------|---------------|-------------------------|---------------------------------------------------------------------|
| `GET`    | `/blogs`      | none (public)           | –                                                                     |
| `GET`    | `/blogs/{id}` | none (public)           | –                                                                     |
| `POST`   | `/blogs`      | `author` or `admin`     | Login required; the creating user becomes the blog's owner.          |
| `PATCH`  | `/blogs/{id}` | `author` or `admin`     | `author` only on a blog they own (API1 fix: ownership check via `BlogService.isAuthor`); `admin` on any blog. |
| `DELETE` | `/blogs/{id}` | `admin`                 | Replaces the shared-secret header entirely; only `admin` may delete. |

Implementation note: `BlogResource` is `@DenyAll`, so a new endpoint is closed until it is released
explicitly. `@PermitAll` on the two `GET` methods, `@RolesAllowed({"author", "admin"})` on
`addBlog`/`updateBlog` and `@RolesAllowed("admin")` on `deleteBlog` cover the role check. The
*ownership* part of the `PATCH` rule needs to know who is calling, which `@RolesAllowed` alone cannot
express: `BlogResource` reads the username from `SecurityIdentity`, `addBlog` stores it as the blog's
`author`, and `updateBlog` answers `403` unless the caller is `admin` or `BlogService.isAuthor` is
true. A blog without an `author` can only be edited by an `admin`. The two seeded blogs belong to
`bob` and `alice`.

## Tests

The services are covered by plain `@QuarkusTest` classes, the REST layer by `BlogResourceTest`,
which drives the API over HTTP with **REST Assured** (`io.rest-assured:rest-assured`, test scope in
`pom.xml`). Those tests never touch a service or entity class, they send JSON and assert on status
codes, headers and the response body, which is exactly what a client sees:

```java
RestAssured
    .given()
        .body(requestBody)
        .contentType(MediaType.APPLICATION_JSON)
    .when()
        .post("/blogs")
    .then()
        .statusCode(Status.CREATED.getStatusCode())
        .extract().header("Location");
```

`@QuarkusTest` starts the application once and Quarkus points REST Assured at the test port (8081),
so no base URI has to be configured. Covered are the CRUD happy paths, validation errors, paging and
search meta data, the `Location` header and the role and ownership rules. `@TestSecurity`
(`quarkus-test-security`) sets the caller per test, so these tests need no token.

Run them against the local database (`docker-compose up -d`):

```shell script
./mvnw test
```

---
# Local DB
To avoid compatibility issues with Quarkus and Docker hosted in WSL, there is a docker-compose file for a local database setup.
```shell script
# start the database (form the root folder)
docker-compose up -d
```

---
# Keycloak
The `keycloak` service in `docker-compose.yml` imports `src/blog-realm.json` on startup
(`start-dev --import-realm`), so the `blog` realm is ready right after startup, no manual
configuration in the Admin Console is needed.

## Setup

1. **Hostname entry.** Keycloak announces itself as `http://keycloak:8180` (`KC_HOSTNAME_URL`), so
   the token issuer (`iss`) is the same whether the token is requested from the host or from another
   container in `blog-nw`. For the host to resolve that name, add this line to your hosts file
   (`C:\Windows\System32\drivers\etc\hosts` on Windows, `/etc/hosts` on Linux/macOS):

   ```text
   127.0.0.1 keycloak
   ```

2. **Start the container** (from the root folder):

   ```shell script
   docker-compose up -d keycloak
   ```

   The first start takes a moment until the realm is imported. The Admin Console is then available
   at <http://keycloak:8180> (or <http://localhost:8180>), login `admin` / `admin`.

The Keycloak data directory is persisted in the `keycloak_data` volume, so changes made in the
Admin Console survive a container restart. The flip side: `--import-realm` skips a realm that
already exists, so later edits to `src/blog-realm.json` are **not** picked up automatically. To
re-import the file, remove the volume first:

```shell script
docker-compose down
docker volume rm <project>_keycloak_data   # see `docker volume ls` for the exact name
docker-compose up -d keycloak
```

## Realm contents

| Item | Value |
|------|-------|
| Realm | `blog` |
| Realm roles | `reader`, `author`, `admin` (composite over `author`); `reader` is part of `default-roles-blog`, so every user gets it |
| Client | `backend-service`, confidential, secret `secret!`, *Direct Access Grants* (password grant) enabled |

Test users:

| Username | Password | Assigned role | Roles in the token (`realm_access.roles`) |
|----------|----------|---------------|---------------------------------------------|
| `alice`  | `alice`  | `admin`       | `admin`, `author`, `reader`, ...            |
| `bob`    | `bob`    | `author`      | `author`, `reader`, ...                     |

The `...` are Keycloak's built-in defaults (`offline_access`, `uma_authorization`,
`default-roles-blog`).

## Exporting the current configuration

If you changed the realm (clients, roles, users, ...) via the Admin Console and want to persist
those changes back into `src/blog-realm.json` for version control, export the realm from the
running container and copy the result out:

```shell script
docker exec -u root -it keycloak /opt/keycloak/bin/kc.sh export --dir /tmp/export --users realm_file
docker cp keycloak:/tmp/export/blog-realm.json ./src/blog-realm.json
```

The export briefly stops and restarts the embedded Keycloak server, so expect a short
unavailability while the command runs. Review the resulting diff before committing.

## Testing access tokens

Use [HTTPie](https://httpie.io/) to request an access token for one of the imported test users via
the password grant, authenticating as the `backend-service` client:

```shell script
http -v --form --auth backend-service:secret! POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=alice password=alice grant_type=password
http -v --form --auth backend-service:secret! POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=bob password=bob grant_type=password
```

Copy the value of the `access_token` field (without quotes) to your clipboard and paste it into
the debugger at [jwt.io](https://jwt.io). The decoded payload must contain the user's role under
`realm_access.roles`, e.g. for `bob`:

```json
{
  "iss": "http://keycloak:8180/realms/blog",
  "preferred_username": "bob",
  "realm_access": {
    "roles": ["author", "reader", "offline_access", "uma_authorization", "default-roles-blog"]
  }
}
```

If `http` fails with a name resolution error, the hosts entry from [Setup](#setup) is missing.

## Calling the API

These [HTTPie](https://httpie.io/) calls need the running application. Request a token for `bob` and
`alice` as shown in [Testing access tokens](#testing-access-tokens) and store each `access_token`
in a variable, repeat this step once a token has expired:

```shell script
BOB=<access_token of bob>
ALICE=<access_token of alice>
```

Each call below shows the expected status. Replace `<id>` with the `id` from the `POST` response:

```shell script
# reading is public: 200
http :8080/blogs

# writing without a token: 401
http POST :8080/blogs title=Hallo content=Welt

# author creates a blog and becomes its owner: 201
http -A bearer -a "$BOB" POST :8080/blogs title=Hallo content=Welt

# author edits their own blog: 200
http -A bearer -a "$BOB" PATCH :8080/blogs/<id> content='Neuer Inhalt'

# author edits a blog of someone else (blog 2 belongs to alice): 403
http -A bearer -a "$BOB" PATCH :8080/blogs/2 content=Gekapert

# author must not delete, not even their own blog: 403
http -A bearer -a "$BOB" DELETE :8080/blogs/<id>

# admin edits any blog: 200
http -A bearer -a "$ALICE" PATCH :8080/blogs/<id> content='Vom Admin'

# admin deletes any blog: 204
http -A bearer -a "$ALICE" DELETE :8080/blogs/<id>
```

---
# Quarkus 

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/ds1-project-0.1-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST JSON-B ([guide](https://quarkus.io/guides/rest#json-serialisation)): JSON-B serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

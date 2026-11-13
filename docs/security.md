# Security

## OWASP API Top 10 2023

The relevant risks for this API and where the project stands. Reading is anonymous by design, write
access is restricted by role and, for `PATCH`, by blog ownership:

| Risk | State | Notes |
|------|-------|-------|
| **API1 - Broken Object Level Authorization** | covered | `POST` stores the logged-in user as the blog's `author`, and `PATCH` rejects an `author` who does not own the blog with `403` (see [Authorization concept](#authorization-concept)). Reading stays public on purpose, deleting is `admin`-only. |
| **API2 - Broken Authentication** | covered | Write endpoints require a Keycloak/OIDC bearer token. |
| **API3 - Broken Object Property Level Authorization** | covered | The DTOs are the defence here (see [DTOs and mapping](rest-api.md#dtos-and-mapping)). `NewBlogDto`/`UpdateBlogDto` have no `id`, so mass assignment cannot overwrite the key, and `BlogDto` is an explicit whitelist, so a new entity field is not exposed by accident. |
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

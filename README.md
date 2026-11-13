# Blog Project DS1 


![Quarkus](https://img.shields.io/badge/quarkus-%234794EB.svg?style=for-the-badge&logo=quarkus&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/apachemaven-C71A36.svg?style=for-the-badge&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

This is a project for the class 'Distributed Systems 1' at HFTM Grenchen.

---

The currently implemented features are:
- H2 in-memory database, recreated on every start
- Docker-Compose for the local Keycloak setup
- Some blog entities with corresponding repositories, services and tests 
  - DataInitialization.java inserts some initial data into the database
- Basic CRUD operations for the blog entities
- REST API for the blog entities (see [REST API](docs/rest-api.md) and the [Swagger UI](http://localhost:8080/q/swagger-ui))
  - Search and paging with meta data on the list endpoint
  - Bean Validation and a uniform JSON error format
  - Own request/response DTOs instead of the entity (see [DTOs and mapping](docs/rest-api.md#dtos-and-mapping))
  - CORS enabled for browser clients
- Automated tests: service tests plus an HTTP level test of the API with REST Assured


---
# Getting started

The backend needs two surrounding systems:

| System | How it runs | Configuration |
|--------|-------------|---------------|
| **Keycloak** (OIDC, users and roles) | Container `keycloak` from `docker-compose.yml`, imports the realm `src/blog-realm.json` on start | `quarkus.oidc.auth-server-url=http://keycloak:8180/realms/blog`, admin password from `.env` |
| **Database** | H2 in-memory inside the backend, **no container needed**. Recreated on every start, sample data from `DataInitialization` | `quarkus.datasource.*` in `application.properties`, `blog.seed-data.enabled` |

## 1. One-time preparation

Run everything from the root folder.

1. **Hosts entry.** Keycloak announces itself as `http://keycloak:8180`, the host has to resolve that
   name. Add this line to `C:\Windows\System32\drivers\etc\hosts` (Windows, as administrator) or `/etc/hosts`:

   ```text
   127.0.0.1 keycloak
   ```

2. **Secrets.** Copy the template, the `.env` is ignored by Git. `docker-compose` and `./mvnw quarkus:dev`
   both read it. The values match the local realm, use your own in production:

   ```shell script
   cp .env.example .env
   ```

   | Variable | Used by |
   |----------|---------|
   | `QUARKUS_OIDC_CREDENTIALS_SECRET` | Backend, secret of the Keycloak client `backend-service` |
   | `KEYCLOAK_ADMIN_PASSWORD` | Keycloak, password of the Admin Console user `admin` |

## 2a. Run the image (production mode)

```shell script
./mvnw package -DskipTests   # builds the JAR and the image ghcr.io/michael-hftm/blog-backend:latest
docker-compose up -d         # starts Keycloak and the backend in the network blog-nw
```

The backend waits for the Keycloak healthcheck and reaches it inside the network as `keycloak:8180`.
Without `.env`, `docker-compose` refuses to start. Details in [Deployment](docs/deployment.md).

## 2b. Run in dev mode (development)

Start only Keycloak, the backend container would block port 8080:

```shell script
docker-compose up -d keycloak
./mvnw quarkus:dev
```

Either way the API runs on <http://localhost:8080>, the Swagger UI on <http://localhost:8080/q/swagger-ui>.

## 3. Example calls with HTTPie

The realm contains two test users:

| User / password | Role | May |
|-----------------|------|-----|
| `alice` / `alice` | `admin` | create, edit and delete every blog |
| `bob` / `bob` | `author` | create blogs and edit his own, not delete |

**Request a token** via the password grant as client `backend-service` and keep the `access_token`.
A token is valid for a few minutes, request a new one when calls return 401.

```shell script
# bash
BOB=$(http --form --auth backend-service:secret! POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=bob password=bob grant_type=password | python -c "import sys, json; print(json.load(sys.stdin)['access_token'])")
ALICE=$(http --form --auth backend-service:secret! POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=alice password=alice grant_type=password | python -c "import sys, json; print(json.load(sys.stdin)['access_token'])")
```

```powershell
# PowerShell
$BOB = (http --form --auth 'backend-service:secret!' POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=bob password=bob grant_type=password | ConvertFrom-Json).access_token
$ALICE = (http --form --auth 'backend-service:secret!' POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=alice password=alice grant_type=password | ConvertFrom-Json).access_token
```

**Call the API** with the token as Bearer header (`-A bearer -a <token>`). Each call shows the
expected status, replace `<id>` with the `id` from the `POST` response (the calls work the
same in bash and PowerShell):

```shell script
# reading is public: 200
http :8080/blogs searchString==Jedi page==0 size==10

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

# admin deletes any blog: 204
http -A bearer -a "$ALICE" DELETE :8080/blogs/<id>
```

How to inspect a token and the roles it carries is described in [Keycloak](docs/keycloak.md#testing-access-tokens).

---
# Documentation

| Topic | Content |
|-------|---------|
| [REST API](docs/rest-api.md) | Endpoints, DTOs, paging, validation, error format, CORS |
| [Security](docs/security.md) | OWASP API Top 10 assessment and authorization concept (roles, access rules) |
| [Keycloak](docs/keycloak.md) | Setup, realm contents, realm export, access tokens and example API calls |
| [Testing](docs/testing.md) | Service and REST Assured tests |
| [Deployment](docs/deployment.md) | H2 database and production build with Docker |
| [Quarkus](docs/quarkus.md) | Generic Quarkus commands (dev mode, packaging, native build) |

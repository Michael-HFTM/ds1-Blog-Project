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

Run these steps from the root folder to test the auth setup:

1. **Add the hosts entry** once, see [Keycloak setup](docs/keycloak.md#setup).
2. **Start Keycloak** (only Keycloak, the backend container would block port 8080):

   ```shell script
   docker-compose up -d keycloak
   ```

3. **Start the application** once Keycloak has imported the realm, which takes a moment on the first start:

   ```shell script
   ./mvnw quarkus:dev
   ```

The API now runs on <http://localhost:8080>. The test users are listed in
[Realm contents](docs/keycloak.md#realm-contents), example calls are in [Calling the API](docs/keycloak.md#calling-the-api).

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

# Tests

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

They use the H2 in-memory database, no external database is needed:

```shell script
./mvnw test
```

## Black box tests against the packaged application

`PublicBlogResourceIT` extends `PublicBlogResourceTest` and is annotated with
`@QuarkusIntegrationTest`, so the same REST Assured tests run against the finished artifact instead
of the test profile. Since `quarkus.container-image.build=true` is set, Maven first builds the
Docker image and Quarkus starts it as a container in the `prod` profile (Docker has to be running):

```shell script
./mvnw verify -DskipITs=false
```

Only the tests without a logged-in user are part of it. `@TestSecurity` and `@Inject` only work
inside `@QuarkusTest`, against the packaged application the protected endpoints would need a real
Keycloak token. Those cases stay in `BlogResourceTest`.

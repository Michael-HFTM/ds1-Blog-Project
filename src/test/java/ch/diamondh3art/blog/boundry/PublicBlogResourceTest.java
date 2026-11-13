package ch.diamondh3art.blog.boundry;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Blog API tests without a logged-in user. They use nothing but HTTP, so {@link PublicBlogResourceIT}
 * runs them unchanged against the packaged application, where no Keycloak token is available.
 */
@QuarkusTest
class PublicBlogResourceTest {

    @Test
    void anonymousCanReadBlogs() {
        getBody("/blogs", Status.OK);
    }

    @Test
    void anonymousCannotWrite() {
        RestAssured
                .given()
                    .body("{}")
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .post("/blogs")
                .then()
                    .statusCode(Status.UNAUTHORIZED.getStatusCode());

        RestAssured
                .given()
                    .body("{}")
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .patch("/blogs/1")
                .then()
                    .statusCode(Status.UNAUTHORIZED.getStatusCode());

        RestAssured
                .when()
                    .delete("/blogs/1")
                .then()
                    .statusCode(Status.UNAUTHORIZED.getStatusCode())
                    .header("WWW-Authenticate", "Bearer")
                    .body("status", is(Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    void listReturnsEnvelopeWithMetaData() {
        JsonObject page = readObject(getBody("/blogs?page=0&size=1", Status.OK));

        JsonArray data = page.getJsonArray("data");
        JsonObject meta = page.getJsonObject("meta");

        assertEquals(1, data.size());
        assertEquals(1, meta.getInt("size"));
        assertEquals(0, meta.getInt("page"));
        assertEquals(data.size(), meta.getInt("count"));
        assertTrue(meta.getJsonNumber("totalCount").longValue() >= data.size());
    }

    @Test
    void invalidPageSizeReturnsBadRequest() {
        JsonObject error = readObject(getBody("/blogs?size=0", Status.BAD_REQUEST));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), error.getInt("status"));
        assertEquals("size must be between 1 and 100", error.getString("message"));
    }

    @Test
    void getUnknownBlogReturnsJsonError() {
        String responseBody = RestAssured
                .when()
                    .get("/blogs/999999")
                .then()
                    .statusCode(Status.NOT_FOUND.getStatusCode())
                    .header("info", "Blog with id 999999 not found")
                    .extract()
                    .body()
                    .asString();

        JsonObject error = readObject(responseBody);

        assertEquals(Status.NOT_FOUND.getStatusCode(), error.getInt("status"));
        assertEquals("Not Found", error.getString("error"));
        assertEquals("Blog with id 999999 not found", error.getString("message"));
    }

    private String getBody(String path, Status expected) {
        return RestAssured
                .when()
                    .get(path)
                .then()
                    .statusCode(expected.getStatusCode())
                    .extract()
                    .body()
                    .asString();
    }

    private JsonObject readObject(String json) {
        return Json.createReader(new StringReader(json)).readObject();
    }
}

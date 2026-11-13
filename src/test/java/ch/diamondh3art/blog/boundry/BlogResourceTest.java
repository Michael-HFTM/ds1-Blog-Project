package ch.diamondh3art.blog.boundry;

import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.entity.Blog;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.StringReader;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * System test of the blog API: everything goes through HTTP and JSON, no service or entity class
 * is touched. The order only matters for the first two tests, which compare the record count
 * before and after a POST. Runs as admin unless a test says otherwise, so no Keycloak is needed.
 * Tests without a logged-in user live in {@link PublicBlogResourceTest}, because only those also
 * run against the packaged application.
 * Only {@link #foreignBlog} uses the service, because a blog of another user cannot be created
 * while a test runs as a single user.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "alice", roles = {"admin", "author", "reader"})
class BlogResourceTest {

    @Inject
    BlogService blogService;

    @Test
    @TestSecurity(user = "bob", roles = {"author", "reader"})
    void authorCannotPatchForeignOrOwnerlessBlog() {
        patchExpecting(foreignBlog("alice"), Status.FORBIDDEN);
        patchExpecting(foreignBlog(null), Status.FORBIDDEN);
    }

    @Test
    void adminCanPatchForeignBlog() {
        patchExpecting(foreignBlog("bob"), Status.OK);
    }

    @Test
    @TestSecurity(user = "bob", roles = {"author", "reader"})
    void authorPatchingUnknownBlogGetsNotFound() {
        patchExpecting("/blogs/999999", Status.NOT_FOUND);
    }

    @Test
    @TestSecurity(user = "reader", roles = "reader")
    void readerCannotCreateBlog() {
        RestAssured
                .given()
                    .body("{}")
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .post("/blogs")
                .then()
                    .statusCode(Status.FORBIDDEN.getStatusCode())
                    .body("status", is(Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    @TestSecurity(user = "bob", roles = {"author", "reader"})
    void authorCanCreateAndPatchButNotDelete() {
        String location = createBlog("Author", "Darf schreiben");

        RestAssured
                .given()
                    .body("{\"content\": \"Neu\"}")
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .patch(location)
                .then()
                    .statusCode(Status.OK.getStatusCode());

        RestAssured
                .when()
                    .delete(location)
                .then()
                    .statusCode(Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @Order(1)
    void createAndReadBlog() {
        String requestBody = """
                {
                    "title": "Testing",
                    "content": "Testing with Quarkus easy!"
                }
                """;

        String location = RestAssured
                .given()
                    .body(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .post("/blogs")
                .then()
                    .statusCode(Status.CREATED.getStatusCode())
                    .extract()
                    .header("Location");

        String responseBody = RestAssured
                .when()
                    .get(location)
                .then()
                    .statusCode(Status.OK.getStatusCode())
                    .extract()
                    .body()
                    .asString();

        JsonObject blog = readObject(responseBody);

        assertEquals("Testing", blog.getString("title"));
        assertEquals("Testing with Quarkus easy!", blog.getString("content"));
        // the Location header has to point at exactly this record
        assertTrue(location.endsWith("/blogs/" + blog.getJsonNumber("id").longValue()), location);
    }

    @Test
    @Order(2)
    void createdBlogIsCountedInTheListMetaData() {
        long before = totalCount();

        String location = createBlog("Zaehlung", "Erhoeht die Anzahl um eins");

        assertEquals(before + 1, totalCount());

        JsonObject blog = readObject(getBody(location, Status.OK));
        assertEquals("Zaehlung", blog.getString("title"));
    }

    @Test
    void postWithoutTitleReturnsValidationError() {
        String requestBody = """
                {
                    "content": "Titel fehlt"
                }
                """;

        JsonObject error = readObject(postExpectingBadRequest(requestBody));
        JsonObject violation = error.getJsonArray("violations").getJsonObject(0);

        assertEquals("Validation failed", error.getString("message"));
        assertEquals("title", violation.getString("field"));
        assertEquals("Titel darf nicht leer sein.", violation.getString("message"));
    }

    @Test
    void postWithBlankTitleReturnsValidationError() {
        String requestBody = """
                {
                    "title": "   ",
                    "content": "Titel nur Leerzeichen"
                }
                """;

        JsonObject error = readObject(postExpectingBadRequest(requestBody));

        assertEquals("title", error.getJsonArray("violations").getJsonObject(0).getString("field"));
    }

    @Test
    void postWithoutContentReturnsValidationError() {
        String requestBody = """
                {
                    "title": "Blog ohne Inhalt"
                }
                """;

        JsonObject violation = readObject(postExpectingBadRequest(requestBody))
                .getJsonArray("violations")
                .getJsonObject(0);

        assertEquals("content", violation.getString("field"));
        assertEquals("Inhalt darf nicht leer sein.", violation.getString("message"));
    }

    @Test
    void patchWithOnlyContentKeepsTheTitle() {
        String location = createBlog("Partial Patch", "Alter Inhalt");

        String requestBody = """
                {
                    "content": "Neuer Inhalt"
                }
                """;

        String responseBody = RestAssured
                .given()
                    .body(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .patch(location)
                .then()
                    .statusCode(Status.OK.getStatusCode())
                    .extract()
                    .body()
                    .asString();

        JsonObject patched = readObject(responseBody);

        assertEquals("Partial Patch", patched.getString("title"));
        assertEquals("Neuer Inhalt", patched.getString("content"));
    }

    @Test
    void patchWithBlankTitleIsRejectedAndKeepsTheBlog() {
        String location = createBlog("Patch Validation", "Bleibt unveraendert");

        String requestBody = """
                {
                    "title": "   "
                }
                """;

        String responseBody = RestAssured
                .given()
                    .body(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .patch(location)
                .then()
                    .statusCode(Status.BAD_REQUEST.getStatusCode())
                    .header("info", "Validation failed")
                    .extract()
                    .body()
                    .asString();

        JsonObject error = readObject(responseBody);
        assertEquals("title", error.getJsonArray("violations").getJsonObject(0).getString("field"));

        // the rejected patch must not have reached the database
        JsonObject unchanged = readObject(getBody(location, Status.OK));
        assertEquals("Patch Validation", unchanged.getString("title"));
        assertEquals("Bleibt unveraendert", unchanged.getString("content"));
    }

    @Test
    void patchOfUnknownBlogReturnsNotFound() {
        String requestBody = """
                {
                    "content": "Gibt es nicht"
                }
                """;

        RestAssured
                .given()
                    .body(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .patch("/blogs/999999")
                .then()
                    .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void deletingBlogRemovesIt() {
        String location = createBlog("Delete Allowed", "Will be deleted");

        RestAssured
                .when()
                    .delete(location)
                .then()
                    .statusCode(Status.NO_CONTENT.getStatusCode())
                    .body(emptyOrNullString());

        RestAssured
                .when()
                    .get(location)
                .then()
                    .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    private String foreignBlog(String author) {
        Blog blog = new Blog("Fremder Blog", "Gehoert jemand anderem");
        blog.setAuthor(author);
        return "/blogs/" + blogService.addBlog(blog).getId();
    }

    private void patchExpecting(String path, Status expected) {
        RestAssured
                .given()
                    .body("{\"content\": \"Neu\"}")
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .patch(path)
                .then()
                    .statusCode(expected.getStatusCode());
    }

    /** POSTs a blog and returns the Location of the new record. */
    private String createBlog(String title, String content) {
        String requestBody = """
                {
                    "title": "%s",
                    "content": "%s"
                }
                """.formatted(title, content);

        return RestAssured
                .given()
                    .body(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .post("/blogs")
                .then()
                    .statusCode(Status.CREATED.getStatusCode())
                    .extract()
                    .header("Location");
    }

    private String postExpectingBadRequest(String requestBody) {
        return RestAssured
                .given()
                    .body(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .post("/blogs")
                .then()
                    .statusCode(Status.BAD_REQUEST.getStatusCode())
                    .header("info", "Validation failed")
                    .extract()
                    .body()
                    .asString();
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

    /** Records across all pages, read from the meta data of the list endpoint. */
    private long totalCount() {
        return readObject(getBody("/blogs?size=1", Status.OK))
                .getJsonObject("meta")
                .getJsonNumber("totalCount")
                .longValue();
    }

    private JsonObject readObject(String json) {
        return Json.createReader(new StringReader(json)).readObject();
    }
}

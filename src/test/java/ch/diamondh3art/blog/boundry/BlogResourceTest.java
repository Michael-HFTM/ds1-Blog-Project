package ch.diamondh3art.blog.boundry;

import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.entity.Blog;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class BlogResourceTest {

    @Inject
    BlogService blogService;

    @ConfigProperty(name = "blog.delete.secret")
    String blogSecret;

    @Test
    void postWithoutTitleReturnsBadRequest() {
        given()
                .contentType("application/json")
                .body("{\"content\":\"Titel fehlt\"}")
                .when().post("/blogs")
                .then()
                .statusCode(400)
                .header("info", is("Validation failed"))
                .body("status", is(400))
                .body("error", is("Bad Request"))
                .body("message", is("Validation failed"))
                .body("violations[0].field", is("title"))
                .body("violations[0].message", is("Titel darf nicht leer sein."));
    }

    @Test
    void postWithBlankTitleReturnsBadRequest() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"   \",\"content\":\"Titel nur Leerzeichen\"}")
                .when().post("/blogs")
                .then()
                .statusCode(400)
                .body("violations.field", hasItem("title"));
    }

    @Test
    void postWithoutContentReturnsBadRequest() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"Blog ohne Inhalt\"}")
                .when().post("/blogs")
                .then()
                .statusCode(400)
                .body("violations.field", hasItem("content"))
                .body("violations.message", hasItem("Inhalt darf nicht leer sein."));
    }

    @Test
    void patchWithBlankTitleReturnsBadRequestAndKeepsBlog() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Patch Validation", "Bleibt unveraendert"));

        // Act: same error format as the POST above
        given()
                .contentType("application/json")
                .body("{\"title\":\"   \"}")
                .when().patch("/blogs/" + blog.getId())
                .then()
                .statusCode(400)
                .header("info", is("Validation failed"))
                .body("message", is("Validation failed"))
                .body("violations[0].field", is("title"));

        // Assert: the invalid patch was rolled back
        given()
                .when().get("/blogs/" + blog.getId())
                .then()
                .statusCode(200)
                .body("title", is("Patch Validation"));
    }

    @Test
    void patchWithOnlyContentStillWorks() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Partial Patch", "Alter Inhalt"));

        // Act & Assert: a partial body must not trip the title constraint
        given()
                .contentType("application/json")
                .body("{\"content\":\"Neuer Inhalt\"}")
                .when().patch("/blogs/" + blog.getId())
                .then()
                .statusCode(200)
                .body("title", is("Partial Patch"))
                .body("content", is("Neuer Inhalt"));
    }

    @Test
    void postCreatesBlogAndReturnsLocation() {
        // Act
        Response created = given()
                .contentType("application/json")
                .body("{\"title\":\"Created Blog\",\"content\":\"Angelegt via REST\"}")
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .extract().response();

        // Assert: the Location header points at the new resource
        String location = created.getHeader("Location");
        assertTrue(location.endsWith("/blogs/" + created.jsonPath().getLong("id")));

        given()
                .when().get(location)
                .then()
                .statusCode(200)
                .body("title", is("Created Blog"));
    }

    @Test
    void getUnknownBlogReturnsJsonError() {
        given()
                .when().get("/blogs/999999")
                .then()
                .statusCode(404)
                .header("info", endsWith("not found"))
                .body("status", is(404))
                .body("error", is("Not Found"))
                .body("message", is("Blog with id 999999 not found"));
    }

    @Test
    void deleteWithWrongSecretReturnsForbidden() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Delete Forbidden", "Wrong secret"));

        // Act & Assert
        given()
                .header("secret", "wrong-secret")
                .when().delete("/blogs/" + blog.getId())
                .then()
                .statusCode(403)
                .body("message", is("Invalid secret"));
    }

    @Test
    void deleteWithCorrectSecretReturnsNoContent() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Delete Allowed", "Correct secret"));

        // Act & Assert
        given()
                .header("secret", blogSecret)
                .when().delete("/blogs/" + blog.getId())
                .then()
                .statusCode(204)
                .body(is(emptyOrNullString()));

        given()
                .when().get("/blogs/" + blog.getId())
                .then()
                .statusCode(404);
    }

    @Test
    void invalidPageSizeReturnsBadRequest() {
        given()
                .when().get("/blogs?size=0")
                .then()
                .statusCode(400)
                .body("status", is(400))
                .body("message", is("size must be between 1 and 100"));
    }

    @Test
    void listReturnsEnvelopeWithMetaData() {
        given()
                .when().get("/blogs?page=0&size=1")
                .then()
                .statusCode(200)
                .body("data.size()", is(1))
                .body("meta.size", is(1))
                .body("meta.page", is(0))
                .body("meta.totalCount", notNullValue());
    }
}

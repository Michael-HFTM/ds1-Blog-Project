package ch.diamondh3art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import ch.diamondh3art.blog.boundry.PagedResponse;
import ch.diamondh3art.blog.boundry.ResponseException;
import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.control.CommentRepository;
import ch.diamondh3art.blog.control.CommentService;
import ch.diamondh3art.blog.control.UserService;
import ch.diamondh3art.blog.entity.Blog;
import ch.diamondh3art.blog.entity.Comment;
import ch.diamondh3art.blog.entity.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class BlogServiceTest {

    private static final Long UNKNOWN_ID = 999_999L;

    @Inject
    BlogService blogService;

    @Inject
    CommentService commentService;

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserService userService;

    @ConfigProperty(name = "blog.delete.secret")
    String blogSecret;

    @Test
    void listingAndAddingBlogs() {
        // Arrange
        Blog blog = new Blog("Testing Blog", "This is my testing blog");
        long blogsBefore = blogService.getBlogs(Optional.empty(), 0, 20).meta().totalCount();

        // Act
        blogService.addBlog(blog);
        PagedResponse<Blog> blogs = blogService.getBlogs(Optional.empty(), 0, 20);

        // Assert
        assertEquals(blogsBefore + 1, blogs.meta().totalCount());
    }

    @Test
    void pagingReturnsRequestedSlice() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            blogService.addBlog(new Blog("Paged Blog " + i, "Content " + i));
        }

        // Act
        PagedResponse<Blog> firstPage = blogService.getBlogs(Optional.empty(), 0, 2);
        PagedResponse<Blog> secondPage = blogService.getBlogs(Optional.empty(), 1, 2);

        // Assert
        assertEquals(2, firstPage.data().size());
        assertEquals(2, firstPage.meta().count());
        assertEquals(2, secondPage.data().size());
        assertTrue(firstPage.data().stream().map(Blog::getId).noneMatch(
                id -> secondPage.data().stream().anyMatch(other -> other.getId().equals(id))));
    }

    @Test
    void totalCountIgnoresPaging() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            blogService.addBlog(new Blog("Counted Blog " + i, "Content " + i));
        }

        // Act
        PagedResponse<Blog> page = blogService.getBlogs(Optional.empty(), 0, 2);

        // Assert
        assertTrue(page.meta().totalCount() > page.data().size());
        assertEquals(Math.ceil(page.meta().totalCount() / 2.0), page.meta().pageCount());
    }

    @Test
    void pageBeyondLastPageIsEmptyButKeepsTotalCount() {
        // Arrange
        blogService.addBlog(new Blog("Only Blog", "Somewhere on page zero"));

        // Act
        PagedResponse<Blog> page = blogService.getBlogs(Optional.empty(), 9_999, 20);

        // Assert
        assertEquals(0, page.data().size());
        assertTrue(page.meta().totalCount() > 0);
    }

    @Test
    void searchStringFiltersAndAffectsTotalCount() {
        // Arrange
        String unique = "Unique" + UUID.randomUUID().toString().replace("-", "");
        blogService.addBlog(new Blog(unique, "Only this blog carries the marker"));
        blogService.addBlog(new Blog("Unrelated Blog", "No marker here"));

        // Act
        PagedResponse<Blog> found = blogService.getBlogs(Optional.of(unique), 0, 20);

        // Assert
        assertEquals(1, found.meta().totalCount());
        assertEquals(unique, found.data().get(0).getTitle());
    }

    @Test
    void searchStringIsCaseInsensitiveAndMatchesContent() {
        // Arrange
        String unique = "Marker" + UUID.randomUUID().toString().replace("-", "");
        blogService.addBlog(new Blog("Blog With Marker In Content", "The marker is " + unique));

        // Act
        PagedResponse<Blog> found = blogService.getBlogs(Optional.of(unique.toUpperCase()), 0, 20);

        // Assert
        assertEquals(1, found.meta().totalCount());
    }

    @Test
    void blankSearchStringReturnsEverything() {
        // Arrange
        blogService.addBlog(new Blog("Blank Search Blog", "Should still be listed"));
        long total = blogService.getBlogs(Optional.empty(), 0, 20).meta().totalCount();

        // Act
        PagedResponse<Blog> blogs = blogService.getBlogs(Optional.of("   "), 0, 20);

        // Assert
        assertEquals(total, blogs.meta().totalCount());
    }

    @Test
    void invalidPagingParametersThrowBadRequest() {
        // Act & Assert
        assertEquals(400, assertThrows(ResponseException.class,
                () -> blogService.getBlogs(Optional.empty(), -1, 20)).getResponse().getStatus());
        assertEquals(400, assertThrows(ResponseException.class,
                () -> blogService.getBlogs(Optional.empty(), 0, 0)).getResponse().getStatus());
        assertEquals(400, assertThrows(ResponseException.class,
                () -> blogService.getBlogs(Optional.empty(), 0, 101)).getResponse().getStatus());
    }

    @Test
    void addingBlogAssignsId() {
        // Arrange
        Blog blog = new Blog("Blog With Id", "The id is generated on persist");

        // Act
        Blog added = blogService.addBlog(blog);

        // Assert
        assertNotNull(added.getId());
    }

    @Test
    void gettingBlogById() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Single Blog", "Fetched by its id"));

        // Act
        Blog found = blogService.getBlog(blog.getId());

        // Assert
        assertEquals("Single Blog", found.getTitle());
        assertEquals("Fetched by its id", found.getContent());
    }

    @Test
    void gettingUnknownBlogThrowsNotFound() {
        // Act & Assert
        ResponseException exception = assertThrows(ResponseException.class, () -> blogService.getBlog(UNKNOWN_ID));
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    void updatingOnlyTitleLeavesContentUntouched() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Old Title", "Original content"));

        // Act
        blogService.updateBlog(blog.getId(), new Blog("New Title", null));
        Blog updated = blogService.getBlog(blog.getId());

        // Assert
        assertEquals("New Title", updated.getTitle());
        assertEquals("Original content", updated.getContent());
    }

    @Test
    void updatingOnlyContentLeavesTitleUntouched() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Kept Title", "Old content"));

        // Act
        blogService.updateBlog(blog.getId(), new Blog(null, "New content"));
        Blog updated = blogService.getBlog(blog.getId());

        // Assert
        assertEquals("Kept Title", updated.getTitle());
        assertEquals("New content", updated.getContent());
    }

    @Test
    void updatingUsesPathIdAndIgnoresIdFromBody() {
        // Arrange
        Blog target = blogService.addBlog(new Blog("Target", "Should be updated"));
        Blog other = blogService.addBlog(new Blog("Other", "Should stay untouched"));

        Blog patch = new Blog("Patched", null);
        patch.setId(other.getId());

        // Act
        blogService.updateBlog(target.getId(), patch);

        // Assert
        assertEquals("Patched", blogService.getBlog(target.getId()).getTitle());
        assertEquals("Other", blogService.getBlog(other.getId()).getTitle());
    }

    @Test
    void updatingWithBlankTitleIsRejectedAndRolledBack() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Valid Title", "Valid content"));

        // Act
        ResponseException exception = assertThrows(ResponseException.class,
                () -> blogService.updateBlog(blog.getId(), new Blog("   ", null)));

        // Assert
        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Valid Title", blogService.getBlog(blog.getId()).getTitle());
    }

    @Test
    void addingBlogWithoutContentIsRejectedWithBadRequest() {
        // Act: bypasses the resource's @Valid, so the service has to reject it itself
        ResponseException exception = assertThrows(ResponseException.class,
                () -> blogService.addBlog(new Blog("Blog ohne Inhalt", null)));

        // Assert: 400, not the 500 that a violation caught on flush would produce
        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    void addingBlogWithBlankTitleIsRejectedWithBadRequest() {
        // Act & Assert
        ResponseException exception = assertThrows(ResponseException.class,
                () -> blogService.addBlog(new Blog("   ", "Titel nur Leerzeichen")));
        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    void updatingUnknownBlogThrowsNotFound() {
        // Arrange
        Blog patch = new Blog("Does not matter", null);

        // Act & Assert
        ResponseException exception =
                assertThrows(ResponseException.class, () -> blogService.updateBlog(UNKNOWN_ID, patch));
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    void updatingWithoutIdThrowsBadRequest() {
        // Arrange
        Blog patch = new Blog("Does not matter", null);

        // Act & Assert
        ResponseException exception =
                assertThrows(ResponseException.class, () -> blogService.updateBlog(null, patch));
        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    void deletingBlogRemovesIt() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("To Be Deleted", "Short lived"));
        Long id = blog.getId();

        // Act
        blogService.deleteBlog(id, blogSecret);

        // Assert
        ResponseException exception = assertThrows(ResponseException.class, () -> blogService.getBlog(id));
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    void deletingBlogAlsoRemovesItsComments() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Blog With Comments", "Has comments attached"));
        User author = new User("Delete Test User", "delete.test@mail.com");
        userService.addUser(author);
        commentService.addComment(new Comment("First comment", blog, author));
        commentService.addComment(new Comment("Second comment", blog, author));

        // Act
        blogService.deleteBlog(blog.getId(), blogSecret);

        // Assert
        assertEquals(0, commentRepository.count("blog.id", blog.getId()));
    }

    @Test
    void deletingUnknownBlogThrowsNotFound() {
        // Act & Assert
        ResponseException exception =
                assertThrows(ResponseException.class, () -> blogService.deleteBlog(UNKNOWN_ID, blogSecret));
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    void deletingWithWrongSecretIsForbidden() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("Protected Blog", "Needs the correct secret"));

        // Act
        ResponseException exception =
                assertThrows(ResponseException.class, () -> blogService.deleteBlog(blog.getId(), "wrong-secret"));

        // Assert
        assertEquals(403, exception.getResponse().getStatus());
        assertEquals("Protected Blog", blogService.getBlog(blog.getId()).getTitle());
    }
}

package ch.diamondh3art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.control.CommentRepository;
import ch.diamondh3art.blog.control.CommentService;
import ch.diamondh3art.blog.control.UserService;
import ch.diamondh3art.blog.entity.Blog;
import ch.diamondh3art.blog.entity.Comment;
import ch.diamondh3art.blog.entity.User;
import jakarta.ws.rs.NotFoundException;
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

    @Test
    void listingAndAddingBlogs() {
        // Arrange
        Blog blog = new Blog("Testing Blog", "This is my testing blog");
        int blogsBefore = blogService.getBlogs().size();

        // Act
        blogService.addBlog(blog);
        List<Blog> blogs = blogService.getBlogs();

        // Assert
        assertEquals(blogsBefore + 1, blogs.size());
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
        assertThrows(NotFoundException.class, () -> blogService.getBlog(UNKNOWN_ID));
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
    void updatingUnknownBlogThrowsNotFound() {
        // Arrange
        Blog patch = new Blog("Does not matter", null);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> blogService.updateBlog(UNKNOWN_ID, patch));
    }

    @Test
    void updatingWithoutIdThrowsIllegalArgument() {
        // Arrange
        Blog patch = new Blog("Does not matter", null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> blogService.updateBlog(null, patch));
    }

    @Test
    void deletingBlogRemovesIt() {
        // Arrange
        Blog blog = blogService.addBlog(new Blog("To Be Deleted", "Short lived"));
        Long id = blog.getId();

        // Act
        blogService.deleteBlog(id);

        // Assert
        assertThrows(NotFoundException.class, () -> blogService.getBlog(id));
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
        blogService.deleteBlog(blog.getId());

        // Assert
        assertEquals(0, commentRepository.count("blog.id", blog.getId()));
    }

    @Test
    void deletingUnknownBlogThrowsNotFound() {
        // Act & Assert
        assertThrows(NotFoundException.class, () -> blogService.deleteBlog(UNKNOWN_ID));
    }
}

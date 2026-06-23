package ch.diamondh3art;

import ch.diamondh3art.blog.control.BlogService;
import ch.diamondh3art.blog.control.CommentService;
import ch.diamondh3art.blog.control.UserService;
import ch.diamondh3art.blog.entity.Blog;
import ch.diamondh3art.blog.entity.Comment;
import ch.diamondh3art.blog.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CommentServiceTest {

    @Inject
    CommentService commentService;

    @Inject
    BlogService blogService;

    @Inject
    UserService userService;

    @Test
    void listingAndAddingComments() {
        // Arrange
        Blog blog = new Blog("Comment Test Blog", "This is blog is for testing comments");
        User user = new User("Comment Test User", "comment.test@mail.com");
        blogService.addBlog(blog);
        userService.addUser(user);
        Comment comment = new Comment("This is a test comment", blog, user);
        int commentsBefore = commentService.getComments().size();

        // Act
        commentService.addComment(comment);
        List<Comment> comments = commentService.getComments();

        // Assert
        assertEquals( commentsBefore + 1, comments.size());
    }
}
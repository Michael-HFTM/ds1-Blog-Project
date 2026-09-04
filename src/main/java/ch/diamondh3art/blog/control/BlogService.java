package ch.diamondh3art.blog.control;

import java.util.List;

import ch.diamondh3art.blog.entity.Blog;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class BlogService {
    @Inject
    BlogRepository blogRepository;
    @Inject
    CommentRepository commentRepository;

    @ConfigProperty(name = "blog.delete.secret")
    String blogSecret;

    public List<Blog> getBlogs() {
        var blogs = blogRepository.listAll();
        Log.info("Returning " + blogs.size() + " blogs");
        return blogs;
    }

    public Blog getBlog(Long id) {
        Blog blog = blogRepository.findById(id);
        if (blog == null) {
            throw new NotFoundException("Blog with id " + id + " not found");
        }
        return blog;
    }

    @Transactional
    public Blog addBlog(Blog blog) {
        // TODO: validate blog (we will probably learn this in class)

        Log.info("Adding blog: " + blog.getTitle());
        blogRepository.persist(blog);
        return blog;
    }

    @Transactional
    public Blog updateBlog(Long targetId, Blog blog) {
        // TODO: validate blog (we will probably learn this in class)

        if (targetId == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        Blog existingBlog = blogRepository.findById(targetId);
        if (existingBlog == null) {
            throw new NotFoundException("Blog with id " + targetId + " not found");
        }

        if (blog.getTitle() != null) {
            existingBlog.setTitle(blog.getTitle());
        }

        if (blog.getContent() != null) {
            existingBlog.setContent(blog.getContent());
        }
        Log.info("Updating blog: " + existingBlog.getId());

        return existingBlog;
    }

    @Transactional
    public void deleteBlog(Long id, String secret) {
        if (!blogSecret.equals(secret)) {
            throw new BadRequestException("Invalid secret");
        }

        commentRepository.delete("blog.id", id);
        if (!blogRepository.deleteById(id)) {
            throw new NotFoundException("Blog with id " + id + " not found");
        }
    }
}
package ch.diamondh3art.blog.control;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import ch.diamondh3art.blog.boundry.PagedResponse;
import ch.diamondh3art.blog.boundry.ResponseException;
import ch.diamondh3art.blog.entity.Blog;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class BlogService {

    @Inject
    BlogRepository blogRepository;
    @Inject
    CommentRepository commentRepository;
    @Inject
    Validator validator;
    @ConfigProperty(name = "blog.paging.max-size", defaultValue = "100")
    int maxSize;

    public PagedResponse<Blog> getBlogs(Optional<String> searchString, int page, int size) {
        if (page < 0) {
            throw ResponseException.badRequest("page must not be negative");
        }
        if (size < 1 || size > maxSize) {
            throw ResponseException.badRequest("size must be between 1 and " + maxSize);
        }

        PanacheQuery<Blog> query = blogRepository.search(searchString).page(Page.of(page, size));

        // count() ignores the paging set above, so this is the total over all pages
        long totalCount = query.count();
        List<Blog> blogs = query.list();
        Log.debugf("Returning %d of %d blogs (page %d, size %d, search=%s)",
                blogs.size(), totalCount, page, size, searchString.orElse("-"));

        return PagedResponse.of(blogs, totalCount, query.pageCount(), page, size);
    }

    public Blog getBlog(Long id) {
        Blog blog = blogRepository.findById(id);
        if (blog == null) {
            throw ResponseException.notFound("Blog with id " + id + " not found");
        }
        return blog;
    }

    public boolean isAuthor(Long id, String username) {
        return username.equals(getBlog(id).getAuthor());
    }

    @Transactional
    public Blog addBlog(Blog blog) {
        // @Valid on the resource already covers requests, but callers that bypass it would
        // otherwise only fail on flush - and Quarkus maps that to 500 instead of 400
        validate(blog);

        blogRepository.persist(blog);
        Log.infof("Blog %d created by %s: '%s'", blog.getId(), blog.getAuthor(), blog.getTitle());
        return blog;
    }

    @Transactional
    public Blog updateBlog(Long targetId, Blog blog) {
        if (targetId == null) {
            throw ResponseException.badRequest("id must not be null");
        }

        Blog existingBlog = blogRepository.findById(targetId);
        if (existingBlog == null) {
            throw ResponseException.notFound("Blog with id " + targetId + " not found");
        }

        if (blog.getTitle() != null) {
            existingBlog.setTitle(blog.getTitle());
        }

        if (blog.getContent() != null) {
            existingBlog.setContent(blog.getContent());
        }

        // a PATCH body is partial, so only the merged entity can be validated
        validate(existingBlog);

        Log.infof("Blog %d updated", existingBlog.getId());

        return existingBlog;
    }

    /**
     * Rejects an invalid blog with 400 before it reaches the database. Letting Hibernate catch it
     * on flush would surface as 500, because Bean Validation treats a violation outside of
     * parameter validation as a server error.
     */
    private void validate(Blog blog) {
        Set<ConstraintViolation<Blog>> violations = validator.validate(blog);
        if (!violations.isEmpty()) {
            throw ResponseException.validationFailed(violations);
        }
    }

    @Transactional
    public void deleteBlog(Long id) {
        commentRepository.delete("blog.id", id);
        if (!blogRepository.deleteById(id)) {
            throw ResponseException.notFound("Blog with id " + id + " not found");
        }
        Log.infof("Blog %d deleted together with its comments", id);
    }
}
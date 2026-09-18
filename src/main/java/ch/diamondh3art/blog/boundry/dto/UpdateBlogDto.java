package ch.diamondh3art.blog.boundry.dto;

import ch.diamondh3art.blog.entity.Blog;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Body of a PATCH request. Every field is optional: null means "not sent, keep the stored value".
 * The not-blank rules therefore cannot be checked here - only the merged blog can be validated,
 * which BlogService.updateBlog does.
 */
@Schema(name = "UpdateBlog")
public record UpdateBlogDto(String title, String content) {

    /** Carrier for the fields that were sent; absent fields stay null. */
    public Blog toBlogPatch() {
        return new Blog(title, content);
    }
}

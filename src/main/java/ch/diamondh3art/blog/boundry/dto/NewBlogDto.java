package ch.diamondh3art.blog.boundry.dto;

import ch.diamondh3art.blog.entity.Blog;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Body of a POST request. Both fields are mandatory; the id is assigned by the server and is
 * therefore not part of the request.
 */
@Schema(name = "NewBlog")
public record NewBlogDto(
        @Schema(required = true) String title,
        @Schema(required = true) String content) {

    public Blog toBlog() {
        return new Blog(title, content);
    }
}

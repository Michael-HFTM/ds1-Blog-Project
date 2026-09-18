package ch.diamondh3art.blog.boundry.dto;

import ch.diamondh3art.blog.entity.Blog;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Body of a POST request. Both fields are mandatory; the id is assigned by the server and
 * therefore not part of the request.
 */
@Schema(name = "NewBlog", description = "Everything needed to create a blog post.")
public record NewBlogDto(
        @Schema(required = true, examples = {"Mein erster Blogbeitrag"})
        String title,

        @Schema(required = true, examples = {"Hallo Welt!"})
        String content) {

    public Blog toBlog() {
        return new Blog(title, content);
    }
}

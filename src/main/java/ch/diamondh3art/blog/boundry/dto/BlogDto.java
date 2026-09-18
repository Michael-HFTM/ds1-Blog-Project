package ch.diamondh3art.blog.boundry.dto;

import ch.diamondh3art.blog.entity.Blog;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * What the API returns for a blog. Keeping this separate from the entity means the persistence
 * model can change without breaking clients.
 */
@Schema(name = "Blog", description = "A blog post.")
public record BlogDto(
        @Schema(description = "Assigned by the server", examples = {"1"}, readOnly = true)
        Long id,

        @Schema(description = "Title of the blog", examples = {"Mein erster Blogbeitrag"})
        String title,

        @Schema(description = "Body of the blog", examples = {"Hallo Welt!"})
        String content) {

    public static BlogDto from(Blog blog) {
        return new BlogDto(blog.getId(), blog.getTitle(), blog.getContent());
    }

    public static List<BlogDto> from(List<Blog> blogs) {
        return blogs.stream().map(BlogDto::from).toList();
    }
}

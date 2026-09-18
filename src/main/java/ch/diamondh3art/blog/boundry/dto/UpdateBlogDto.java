package ch.diamondh3art.blog.boundry.dto;

import ch.diamondh3art.blog.entity.Blog;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Body of a PATCH request. Every field is optional: null means "not sent, keep the stored value".
 * The not-blank rules therefore cannot be checked here - only the merged blog can be validated,
 * which BlogService.updateBlog does.
 */
@Schema(name = "UpdateBlog", description = "Partial blog update; omitted fields stay unchanged.")
public record UpdateBlogDto(
        @Schema(description = "New title, or omitted to keep the current one",
                nullable = true, examples = {"Neuer Titel"})
        String title,

        @Schema(description = "New content, or omitted to keep the current one",
                nullable = true, examples = {"Neuer Inhalt"})
        String content) {

    /** Carrier for the fields that were sent; absent fields stay null. */
    public Blog toBlogPatch() {
        return new Blog(title, content);
    }
}

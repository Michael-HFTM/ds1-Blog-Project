package ch.diamondh3art.blog.boundry;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Envelope for paged list endpoints: the records live in {@code data}, everything a client needs
 * to render a pager lives in {@code meta}.
 */
@Schema(name = "PagedResponse", description = "A page of records together with its meta data.")
public record PagedResponse<T>(List<T> data, PageMeta meta) {

    /**
     * @param totalCount number of matching records across all pages
     * @param pageCount  number of pages available for the requested page size
     * @param count      number of records in this response (smaller than size on the last page)
     */
    @Schema(name = "PageMeta", description = "Everything a client needs to render a pager.")
    public record PageMeta(
            @Schema(description = "Matching records across all pages", examples = {"42"})
            long totalCount,
            @Schema(description = "Pages available for the requested size", examples = {"9"})
            int pageCount,
            @Schema(description = "Zero-based index of this page", examples = {"1"})
            int page,
            @Schema(description = "Requested records per page", examples = {"5"})
            int size,
            @Schema(description = "Records in this response, smaller than size on the last page",
                    examples = {"5"})
            int count) {
    }

    public static <T> PagedResponse<T> of(List<T> data, long totalCount, int pageCount, int page, int size) {
        return new PagedResponse<>(data, new PageMeta(totalCount, pageCount, page, size, data.size()));
    }
}

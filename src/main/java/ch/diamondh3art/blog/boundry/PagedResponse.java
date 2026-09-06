package ch.diamondh3art.blog.boundry;

import java.util.List;

/**
 * Envelope for paged list endpoints: the records live in {@code data}, everything a client needs
 * to render a pager lives in {@code meta}.
 */
public record PagedResponse<T>(List<T> data, PageMeta meta) {

    /**
     * @param totalCount number of matching records across all pages
     * @param pageCount  number of pages available for the requested page size
     * @param count      number of records in this response (smaller than size on the last page)
     */
    public record PageMeta(long totalCount, int pageCount, int page, int size, int count) {
    }

    public static <T> PagedResponse<T> of(List<T> data, long totalCount, int pageCount, int page, int size) {
        return new PagedResponse<>(data, new PageMeta(totalCount, pageCount, page, size, data.size()));
    }
}

package ch.diamondh3art.blog.control;

import ch.diamondh3art.blog.entity.Blog;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class BlogRepository implements PanacheRepository<Blog> {

    /**
     * Builds the query for the blog list. Returns a {@link PanacheQuery} instead of a
     * List so that paging (query.page(...)) can be added later without changing this method.
     */
    public PanacheQuery<Blog> search(Optional<String> searchString) {
        if (searchString.isEmpty() || searchString.get().isBlank()) {
            return findAll(Sort.by("id"));
        }

        String pattern = "%" + searchString.get().strip().toLowerCase() + "%";
        return find("lower(title) like :search or lower(content) like :search",
                Sort.by("id"),
                Parameters.with("search", pattern));
    }
}

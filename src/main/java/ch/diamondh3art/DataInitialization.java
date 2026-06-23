package ch.diamondh3art;

import ch.diamondh3art.blog.control.BlogRepository;
import ch.diamondh3art.blog.entity.Blog;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DataInitialization {

    @Inject
    BlogRepository blogRepository;

    @Transactional
    public void init(@Observes StartupEvent event) {
        if (blogRepository.count() == 0) {
            Blog blog = new Blog("My first blog", "This is my first blog");
            blogRepository.persist(blog);
        }
    }
}

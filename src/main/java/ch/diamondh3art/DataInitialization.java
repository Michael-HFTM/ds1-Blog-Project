package ch.diamondh3art;

import ch.diamondh3art.blog.control.BlogRepository;
import ch.diamondh3art.blog.control.CommentRepository;
import ch.diamondh3art.blog.control.UserRepository;
import ch.diamondh3art.blog.entity.Blog;
import ch.diamondh3art.blog.entity.Comment;
import ch.diamondh3art.blog.entity.User;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DataInitialization {

    @Inject
    BlogRepository blogRepository;
    @Inject
    CommentRepository commentRepository;
    @Inject
    UserRepository userRepository;

    @Transactional
    public void init(@Observes StartupEvent event) {
        if (blogRepository.count() == 0) {

            User user1 = new User("Obi Wan Kenobi", "obi-wan.kenobi@federation.com");
            User user2 = new User("Darth Vader", "darth.vader@empire.com");
            userRepository.persist(user1);
            userRepository.persist(user2);

            Blog blog1 = new Blog("How i was granted the rank of Jedi master.", "So basically, I was granted the rank of Jedi master because i'm a gigachad.");
            Blog blog2 = new Blog("I was denied the title of Jedi Master.", "They say im not ready. But i'm the chosen one!");
            blogRepository.persist(blog1);
            blogRepository.persist(blog2);

            Comment comment1 = new Comment("Skill issue.", blog2, user1);
            commentRepository.persist(comment1);
        }
    }
}

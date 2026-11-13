package ch.diamondh3art;

import ch.diamondh3art.blog.control.BlogRepository;
import ch.diamondh3art.blog.control.CommentRepository;
import ch.diamondh3art.blog.control.UserRepository;
import ch.diamondh3art.blog.entity.Blog;
import ch.diamondh3art.blog.entity.Comment;
import ch.diamondh3art.blog.entity.User;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DataInitialization {

    @Inject
    BlogRepository blogRepository;
    @Inject
    CommentRepository commentRepository;
    @Inject
    UserRepository userRepository;
    @ConfigProperty(name = "blog.seed-data.enabled", defaultValue = "true")
    boolean seedDataEnabled;

    @ConfigProperty(name = "quarkus.datasource.db-kind")
    String dbKind;
    // the issuer URL is public, unlike the client secret which must never be logged
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String oidcServerUrl;

    @Transactional
    public void init(@Observes StartupEvent event) {
        Log.infof("Blog backend starting in %s mode (database: %s, OIDC: %s)",
                LaunchMode.current(), dbKind, oidcServerUrl);

        if (!seedDataEnabled) {
            Log.info("Seeding sample data is disabled (blog.seed-data.enabled=false)");
        } else if (blogRepository.count() == 0) {
            Log.info("Database is empty, seeding sample data");

            User user1 = new User("Obi Wan Kenobi", "obi-wan.kenobi@federation.com");
            User user2 = new User("Darth Vader", "darth.vader@empire.com");
            userRepository.persist(user1);
            userRepository.persist(user2);

            Blog blog1 = new Blog("How i was granted the rank of Jedi master.", "So basically, I was granted the rank of Jedi master because i'm a gigachad.");
            Blog blog2 = new Blog("I was denied the title of Jedi Master.", "They say im not ready. But i'm the chosen one!");
            blog1.setAuthor("bob");
            blog2.setAuthor("alice");
            blogRepository.persist(blog1);
            blogRepository.persist(blog2);

            Comment comment1 = new Comment("Skill issue.", blog2, user1);
            commentRepository.persist(comment1);
        }
        Log.infof("Data ready: %d users, %d blogs, %d comments",
                userRepository.count(), blogRepository.count(), commentRepository.count());
    }
}

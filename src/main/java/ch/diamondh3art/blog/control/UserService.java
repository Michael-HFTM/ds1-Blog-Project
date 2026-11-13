package ch.diamondh3art.blog.control;

import ch.diamondh3art.blog.entity.User;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class UserService {
    @Inject
    UserRepository userRepository;

    public List<User> getUsers() {
        var users = userRepository.listAll();
        Log.debugf("Returning %d users", users.size());
        return users;
    }

    @Transactional
    public void addUser(User user) {
        userRepository.persist(user);
        // no e-mail address in the log, it is personal data
        Log.infof("User %d added: %s", user.getId(), user.getName());
    }
}
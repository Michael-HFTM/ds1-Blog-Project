package ch.diamondh3art;

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
        Log.info("Returning " + users.size() + " blogs");
        return users;
    }

    @Transactional
    public void addUser(User user) {
        Log.info("Adding blog " + user.getName());
        userRepository.persist(user);
    }
}
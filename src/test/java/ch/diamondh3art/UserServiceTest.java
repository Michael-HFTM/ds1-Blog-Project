package ch.diamondh3art;

import ch.diamondh3art.blog.control.UserService;
import ch.diamondh3art.blog.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class UserServiceTest {

    @Inject
    UserService userService;

    @Test
    void listingAndAddingUsers() {
        // Arrange
        User user = new User("Max Muster", "max.muster@mail.com");
        int usersBefore = userService.getUsers().size();

        // Act
        userService.addUser(user);
        List<User> users = userService.getUsers();

        // Assert
        assertEquals(usersBefore + 1, users.size());
    }
}
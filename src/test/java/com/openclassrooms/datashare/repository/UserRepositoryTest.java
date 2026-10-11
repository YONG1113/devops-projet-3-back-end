package com.openclassrooms.datashare.repository;

import com.openclassrooms.datashare.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends PostgreSQLRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByLoginReturnsUser() {
        User saved = userRepository.saveAndFlush(user());

        Optional<User> result = userRepository.findByLogin("john");

        assertThat(result).contains(saved);
    }

    @Test
    void findByLoginReturnsEmptyForUnknownLogin() {
        assertThat(userRepository.findByLogin("unknown")).isEmpty();
    }

    private User user() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("john");
        user.setPassword("encoded-password");
        return user;
    }
}

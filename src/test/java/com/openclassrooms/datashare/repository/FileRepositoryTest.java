package com.openclassrooms.datashare.repository;

import com.openclassrooms.datashare.entities.File;
import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.support.PostgreSQLIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FileRepositoryTest extends PostgreSQLIntegrationTest {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.saveAndFlush(user("owner@example.com"));
        anotherUser = userRepository.saveAndFlush(user("other@example.com"));
    }

    @Test
    void findsAFileByBucketObjectPathAndOwnerLogin() {
        File saved = fileRepository.saveAndFlush(file(owner, "documents", "users/1/report.pdf",
                "token-one", Instant.now().plusSeconds(3600)));

        assertThat(fileRepository.findByBucketAndObjectPathAndUserLogin(
                "documents", "users/1/report.pdf", owner.getLogin())).contains(saved);
        assertThat(fileRepository.findByBucketAndObjectPathAndUserLogin(
                "documents", "users/1/report.pdf", anotherUser.getLogin())).isEmpty();
    }

    @Test
    void findsAFileByDownloadTokenHash() {
        File saved = fileRepository.saveAndFlush(file(owner, "documents", "users/1/photo.jpg",
                "token-two", Instant.now().plusSeconds(3600)));

        assertThat(fileRepository.findByDownloadTokenHash("token-two")).contains(saved);
        assertThat(fileRepository.findByDownloadTokenHash("unknown-token")).isEmpty();
    }

    @Test
    void listsOnlyTheOwnersFilesInCreationOrder() {
        File first = fileRepository.saveAndFlush(file(owner, "documents", "users/1/first.txt",
                "token-three", Instant.now().plusSeconds(3600)));
        File second = fileRepository.saveAndFlush(file(owner, "documents", "users/1/second.txt",
                "token-four", Instant.now().plusSeconds(7200)));
        fileRepository.saveAndFlush(file(anotherUser, "documents", "users/2/private.txt",
                "token-five", Instant.now().plusSeconds(3600)));

        assertThat(fileRepository.findAllByUserLoginOrderByCreatedAtAsc(owner.getLogin()))
                .containsExactly(first, second);
    }

    @Test
    void findsAFileByIdOnlyForItsOwner() {
        File saved = fileRepository.saveAndFlush(file(owner, "documents", "users/1/archive.zip",
                "token-six", Instant.now().plusSeconds(3600)));

        assertThat(fileRepository.findByIdAndUserLogin(saved.getId(), owner.getLogin())).contains(saved);
        assertThat(fileRepository.findByIdAndUserLogin(saved.getId(), anotherUser.getLogin())).isEmpty();
    }

    @Test
    void listsOnlyExpiredFilesOrderedByExpiration() {
        Instant now = Instant.now();
        File oldest = fileRepository.saveAndFlush(file(owner, "documents", "users/1/oldest.txt",
                "token-seven", now.minusSeconds(120)));
        File newest = fileRepository.saveAndFlush(file(owner, "documents", "users/1/newest.txt",
                "token-eight", now.minusSeconds(60)));
        fileRepository.saveAndFlush(file(owner, "documents", "users/1/active.txt",
                "token-nine", now.plusSeconds(3600)));

        assertThat(fileRepository.findByExpiresAtLessThanEqualOrderByExpiresAtAsc(now))
                .containsExactly(oldest, newest);
    }

    private User user(String login) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setLogin(login);
        user.setPassword("encoded-password");
        return user;
    }

    private File file(User user, String bucket, String objectPath, String token, Instant expiresAt) {
        File file = new File();
        file.setUser(user);
        file.setOriginalName(objectPath.substring(objectPath.lastIndexOf('/') + 1));
        file.setBucket(bucket);
        file.setObjectPath(objectPath);
        file.setSize(10L);
        file.setContentType("application/octet-stream");
        file.setExpiresAt(expiresAt);
        file.setDownloadTokenHash(token);
        return file;
    }
}

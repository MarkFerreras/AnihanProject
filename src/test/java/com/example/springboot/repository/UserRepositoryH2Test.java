package com.example.springboot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.model.User;

/**
 * Real-JPA uniqueness checks for {@link UserRepository}.
 * ISO 25010 characteristic: Reliability (data integrity) and Security (account identity).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:userRepoDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserRepositoryH2Test {

    @Autowired private UserRepository repo;

    private User user(String username, String email) {
        User u = new User(username, "hash", email, "ROLE_TRAINER");
        u.setLastName("L");
        u.setFirstName("F");
        u.setMiddleName("M");
        u.setBirthdate(LocalDate.of(1990, 1, 1));
        u.setAge(36);
        return u;
    }

    @Test
    void duplicateUsernameIsRejected() {
        repo.saveAndFlush(user("trainer1", "a@x.com"));
        assertThrows(DataIntegrityViolationException.class,
                () -> repo.saveAndFlush(user("trainer1", "b@x.com")));
    }

    @Test
    void existsByEmailGuardDetectsExistingEmail() {
        repo.saveAndFlush(user("trainer1", "a@x.com"));
        assertTrue(repo.existsByEmail("a@x.com"));
        assertFalse(repo.existsByEmail("zzz@x.com"));
    }

    @Test
    void duplicateEmail_currentlyNotRejectedByEntityMappedSchema() {
        // FINDING: schema.sql declares UNIQUE KEY uq_email, but User.email has no unique=true, so a
        // schema generated from the entities (as in these tests) lets a duplicate email through.
        // Production MySQL is protected by schema.sql only; the app-level guard is existsByEmail.
        repo.saveAndFlush(user("trainer1", "dup@x.com"));
        repo.saveAndFlush(user("trainer2", "dup@x.com"));
        assertEquals(2, repo.count());
    }
}

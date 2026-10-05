package com.example.springboot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.model.SystemLog;

/**
 * Real-JPA checks of the timestamp-range query of {@link SystemLogRepository}.
 * ISO 25010 characteristic: Functional suitability (correctness) and Security (audit accountability).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:systemLogRepoDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class SystemLogRepositoryH2Test {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 1, 8, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 10, 1, 18, 0, 0);

    @Autowired private SystemLogRepository repo;

    private void log(String action, LocalDateTime at) {
        SystemLog l = new SystemLog(1, "admin", "ROLE_ADMIN", action, "127.0.0.1");
        l.setTimestamp(at);
        repo.saveAndFlush(l);
    }

    @Test
    void systemLogRangeQueryIsInclusiveAtBoundaries() {
        log("before", FROM.minusSeconds(1));
        log("atFrom", FROM);
        log("middle", FROM.plusHours(1));
        log("atTo", TO);
        log("after", TO.plusSeconds(1));

        List<SystemLog> rows = repo.findByTimestampBetweenOrderByTimestampDesc(FROM, TO);

        assertEquals(List.of("atTo", "middle", "atFrom"), rows.stream().map(SystemLog::getAction).toList());
    }

    @Test
    void systemLogRangeQueryEmptyRange() {
        log("elsewhere", FROM.plusHours(2));
        assertTrue(repo.findByTimestampBetweenOrderByTimestampDesc(FROM, FROM).isEmpty());
    }

    @Test
    void systemLogRangeQueryWithEqualBoundsMatchesExactRow() {
        log("exact", FROM);
        assertEquals(1, repo.findByTimestampBetweenOrderByTimestampDesc(FROM, FROM).size());
    }

    @Test
    void findAllOrderedNewestFirst() {
        log("old", FROM);
        log("new", TO);
        assertEquals("new", repo.findAllByOrderByTimestampDesc().get(0).getAction());
    }
}

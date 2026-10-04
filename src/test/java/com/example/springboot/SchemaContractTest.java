package com.example.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.springboot.model.Document;

import jakarta.persistence.Column;

class SchemaContractTest {
    @Test
    void documentFileTypeSupportsOpenXmlMimeTypesEverywhere() throws Exception {
        Column column = Document.class.getDeclaredField("fileType").getAnnotation(Column.class);
        assertEquals(100, column.length());

        for (String file : List.of("src/main/sql/schema.sql", "src/main/sql/AnihanSRMS.sql")) {
            String sql = Files.readString(Path.of(file));
            assertTrue(sql.matches("(?s).*file_type\\s+VARCHAR\\(100\\)\\s+NOT NULL.*"), file);
        }
        assertTrue(Files.exists(Path.of(
                "src/main/sql/migrations/2026-09-22-widen-documents-file-type.sql")));
    }

    @Test
    void demoAccountSeedOnlyInsertsMissingUsers() throws Exception {
        Path seed = Path.of("src/main/sql/seed-accounts.sql");
        assertTrue(Files.exists(seed));
        String sql = Files.readString(seed).toLowerCase();
        assertTrue(sql.contains("where not exists"));
        assertTrue(sql.contains("timestampdiff(year"));
        for (String username : List.of("admin", "registrar", "trainer")) {
            assertTrue(sql.contains("username = '" + username + "'"), username);
        }
        assertTrue(!sql.contains("update users"));
        assertTrue(!sql.contains("delete from users"));
        assertTrue(!sql.contains("delete from user_security_answers"));
        assertTrue(!sql.contains("on duplicate key update"));
    }

    @Test
    void soChecklistColumnsExistInEverySchemaCopy() throws Exception {
        for (String file : List.of("src/main/sql/schema.sql", "src/main/sql/AnihanSRMS.sql",
                "src/test/resources/document-storage-h2-schema.sql")) {
            String sql = Files.readString(Path.of(file));
            assertTrue(sql.matches("(?s).*enrollment_date DATE NULL,\\s+completion_date DATE NULL,.*"),
                    file + ": completion_date must follow enrollment_date");
            assertTrue(sql.matches("(?s).*student_status VARCHAR\\(25\\) NOT NULL DEFAULT 'Enrolling',"
                            + "\\s+employment_status VARCHAR\\(25\\) NULL,.*"),
                    file + ": employment_status must follow student_status");
        }
        assertTrue(Files.exists(Path.of("src/main/sql/migrations/2026-10-03-so-checklist.sql")));
    }
}

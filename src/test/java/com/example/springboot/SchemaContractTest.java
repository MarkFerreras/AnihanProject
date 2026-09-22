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
}

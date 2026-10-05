package com.example.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.Modifying;

import com.example.springboot.model.Document;
import com.example.springboot.repository.SystemLogRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Pins the SQL schema files against the JPA entities and the migrations (JPA DDL is {@code none}, so
 * these text checks are the only drift guard).
 * <p>
 * ISO 25010: Maintainability (analysability) and Reliability (data integrity).
 */
class SchemaContractTest {

    private static final String SCHEMA = "src/main/sql/schema.sql";
    private static final String DUMP = "src/main/sql/AnihanSRMS.sql";
    private static final Set<String> NON_COLUMN_WORDS = Set.of("PRIMARY", "UNIQUE", "FOREIGN", "CONSTRAINT",
            "KEY", "INDEX", "CHECK", "FULLTEXT");

    private static List<Class<?>> entities() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<Class<?>> result = new ArrayList<>();
        for (var def : scanner.findCandidateComponents("com.example.springboot.model")) {
            result.add(Class.forName(def.getBeanClassName()));
        }
        return result;
    }

    private static Set<String> tableNames(String sql) {
        Set<String> names = new TreeSet<>();
        Matcher m = Pattern.compile("(?i)CREATE TABLE\\s+(?:IF NOT EXISTS\\s+)?`?(\\w+)`?\\s*\\(").matcher(sql);
        while (m.find()) {
            names.add(m.group(1).toLowerCase(Locale.ROOT));
        }
        return names;
    }

    /** The text between {@code CREATE TABLE name (} and the closing line starting with {@code )}. */
    private static String tableBlock(String sql, String table) {
        Matcher m = Pattern.compile("(?i)CREATE TABLE\\s+(?:IF NOT EXISTS\\s+)?`?" + table + "`?\\s*\\(").matcher(sql);
        assertTrue(m.find(), "no CREATE TABLE for " + table);
        int end = sql.indexOf("\n)", m.end());
        assertTrue(end > 0, "unterminated CREATE TABLE " + table);
        return sql.substring(m.end(), end);
    }

    private static Set<String> columnsOf(String block) {
        Set<String> cols = new TreeSet<>();
        Matcher m = Pattern.compile("(?m)^\\s+`?(\\w+)`?\\s+[A-Za-z]+(?:\\(|\\s|,|\\r?$)").matcher(block);
        while (m.find()) {
            if (!NON_COLUMN_WORDS.contains(m.group(1).toUpperCase(Locale.ROOT))) {
                cols.add(m.group(1).toLowerCase(Locale.ROOT));
            }
        }
        return cols;
    }

    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static String stripSqlComments(String sql) {
        return sql.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)--.*$", "");
    }

    private static List<String> statements(String sql) {
        List<String> list = new ArrayList<>();
        for (String raw : stripSqlComments(sql).split(";")) {
            String t = raw.trim();
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        return list;
    }

    private static List<Path> migrations() throws Exception {
        try (Stream<Path> files = Files.list(Path.of("src/main/sql/migrations"))) {
            return files.filter(f -> f.toString().endsWith(".sql")).sorted().toList();
        }
    }

    private static String abbreviate(String s) {
        return s.length() > 90 ? s.substring(0, 90) + "..." : s;
    }

    /** T10-01 */
    @Test
    void everyEntityTableExistsInSchemaSql() throws Exception {
        Set<String> tables = tableNames(Files.readString(Path.of(SCHEMA)));
        List<Class<?>> entities = entities();
        assertTrue(entities.size() >= 20, "expected the 20 entities, found " + entities.size());
        for (Class<?> entity : entities) {
            Table table = entity.getAnnotation(Table.class);
            assertTrue(table != null, entity.getSimpleName() + " has no @Table");
            assertTrue(tables.contains(table.name()), entity.getSimpleName() + " -> " + table.name());
        }
    }

    /** T10-02 */
    @Test
    void everyEntityColumnExistsInItsTable() throws Exception {
        String sql = Files.readString(Path.of(SCHEMA));
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Class<?> entity : entities()) {
            Set<String> cols = columnsOf(tableBlock(sql, entity.getAnnotation(Table.class).name()));
            for (var field : entity.getDeclaredFields()) {
                if (field.isAnnotationPresent(Transient.class)
                        || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String name = null;
                Column column = field.getAnnotation(Column.class);
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                if (join != null && !join.name().isEmpty()) {
                    name = join.name();
                } else if (column != null) {
                    name = column.name().isEmpty() ? snake(field.getName()) : column.name();
                }
                if (name == null) {
                    continue;
                }
                checked++;
                if (!cols.contains(name.toLowerCase(Locale.ROOT))) {
                    missing.add(entity.getSimpleName() + "." + field.getName() + " -> " + name);
                }
            }
        }
        assertTrue(checked > 100, "column scan looks broken, only " + checked + " checked");
        assertEquals(List.of(), missing);
    }

    /**
     * T10-03. FINDING: AnihanSRMS.sql (the device-transfer dump) predates the class-management and
     * security-question tables, so it is not equal to schema.sql. The gap is pinned explicitly.
     */
    @Test
    void schemaSqlAndAnihanSrmsSqlDefineSameTables_currentlyDumpLacksFourTables() throws Exception {
        Set<String> schema = tableNames(Files.readString(Path.of(SCHEMA)));
        Set<String> dump = tableNames(Files.readString(Path.of(DUMP)));
        Set<String> missingFromDump = new TreeSet<>(schema);
        missingFromDump.removeAll(dump);
        assertEquals(Set.of("class_enrollments", "classes", "security_questions", "user_security_answers"),
                missingFromDump);
        Set<String> extraInDump = new TreeSet<>(dump);
        extraInDump.removeAll(schema);
        assertEquals(Set.of(), extraInDump);
    }

    /** T10-04 */
    @Test
    void systemLogsTimestampIndexExists() throws Exception {
        for (String file : List.of(SCHEMA, DUMP)) {
            String block = tableBlock(Files.readString(Path.of(file)), "system_logs");
            assertTrue(Pattern.compile("(?i)INDEX\\s+\\w+\\s*\\(\\s*`?timestamp`?").matcher(block).find(), file);
        }
    }

    /** T10-05 */
    @Test
    void studentIdNotNullUniqueAndStudentNumberNullableUnique() throws Exception {
        for (String file : List.of(SCHEMA, "src/test/resources/repository-h2-schema.sql")) {
            String block = tableBlock(Files.readString(Path.of(file)), "student_records");
            assertTrue(Pattern.compile("(?m)^\\s+student_id\\s+VARCHAR\\(20\\)\\s+NOT NULL\\b").matcher(block).find(),
                    file + ": student_id must be NOT NULL");
            assertTrue(Pattern.compile("(?m)^\\s+student_number\\s+VARCHAR\\(20\\)\\s+NULL\\b").matcher(block).find(),
                    file + ": student_number must be NULL-able");
            assertTrue(Pattern.compile("(?i)UNIQUE\\s+(KEY\\s+\\w+\\s*)?\\(\\s*student_id\\s*\\)").matcher(block).find(),
                    file + ": student_id must be UNIQUE");
            assertTrue(Pattern.compile("(?i)UNIQUE\\s+(KEY\\s+\\w+\\s*)?\\(\\s*student_number\\s*\\)").matcher(block).find(),
                    file + ": student_number must be UNIQUE");
        }
    }

    /**
     * T10-05 (dump). FINDING: AnihanSRMS.sql has no student_records.student_number column or unique
     * key at all (it predates migration 2026-08-27-add-student-number.sql), while student_id is still
     * NOT NULL UNIQUE. A device restored from the dump would break the student-number feature.
     */
    @Test
    void studentIdNotNullUniqueAndStudentNumberNullableUnique_currentlyDumpLacksStudentNumber() throws Exception {
        String block = tableBlock(Files.readString(Path.of(DUMP)), "student_records");
        assertTrue(Pattern.compile("(?m)^\\s+student_id\\s+VARCHAR\\(20\\)\\s+NOT NULL\\b").matcher(block).find());
        assertTrue(Pattern.compile("(?i)UNIQUE\\s+KEY\\s+\\w+\\s*\\(\\s*student_id\\s*\\)").matcher(block).find());
        assertTrue(!block.contains("student_number"));
    }

    /**
     * T10-06. Text heuristic over every statement that is not wrapped in a guarded
     * {@code SET @sql = IF(...)} / {@code PREPARE}: CREATE TABLE needs IF NOT EXISTS, DROP TABLE needs
     * IF EXISTS, a bare ADD/DROP in ALTER TABLE is rejected, INSERT needs IGNORE or WHERE NOT EXISTS,
     * and a bare DELETE is rejected.
     * <p>
     * FINDING: 2026-05-20-sync-and-clear-students.sql unconditionally deletes every student table on
     * each run (an intentional one-off wipe, so not idempotent), and
     * 2026-09-19-purge-login-logout-logs.sql deletes log rows by filter. Both are pinned in an
     * explicit allow-list.
     */
    @Test
    void migrationsAreIdempotentByConstruction() throws Exception {
        Set<String> allowedBareDelete = Set.of("2026-05-20-sync-and-clear-students.sql",
                "2026-09-19-purge-login-logout-logs.sql");
        List<String> violations = new ArrayList<>();
        Set<String> bareDeleteFiles = new TreeSet<>();
        List<Path> files = migrations();
        assertTrue(files.size() >= 16, "expected the migrations folder to be populated");
        for (Path file : files) {
            String name = file.getFileName().toString();
            for (String st : statements(Files.readString(file))) {
                String up = st.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
                if (up.startsWith("CREATE TABLE") && !up.startsWith("CREATE TABLE IF NOT EXISTS")) {
                    violations.add(name + ": " + abbreviate(up));
                } else if (up.startsWith("DROP TABLE") && !up.startsWith("DROP TABLE IF EXISTS")) {
                    violations.add(name + ": " + abbreviate(up));
                } else if (up.matches("ALTER TABLE \\w+ (ADD|DROP) .*")) {
                    violations.add(name + ": " + abbreviate(up));
                } else if (up.startsWith("INSERT") && !up.startsWith("INSERT IGNORE")
                        && !up.contains("WHERE NOT EXISTS")) {
                    violations.add(name + ": " + abbreviate(up));
                } else if (up.startsWith("DELETE")) {
                    bareDeleteFiles.add(name);
                }
            }
        }
        assertEquals(List.of(), violations);
        assertEquals(allowedBareDelete, bareDeleteFiles);
    }

    /** T10-07 */
    @Test
    void systemLogsHasNoUpdateOrDeleteInCode() throws Exception {
        Pattern mutate = Pattern.compile(
                "(?i)\\b(UPDATE|DELETE\\s+FROM|TRUNCATE(\\s+TABLE)?)\\s+(system_logs|SystemLog)\\b"
                        + "|systemLogRepository\\s*\\.\\s*(delete|remove)");
        List<String> hits = new ArrayList<>();
        List<Path> scanned = new ArrayList<>();
        try (Stream<Path> java = Files.walk(Path.of("src/main/java"))) {
            java.filter(f -> f.toString().endsWith(".java")).forEach(scanned::add);
        }
        scanned.addAll(List.of(Path.of(SCHEMA), Path.of("src/main/sql/seed-accounts.sql")));
        scanned.addAll(migrations());
        for (Path file : scanned) {
            String name = file.getFileName().toString();
            if (name.equals("2026-09-19-purge-login-logout-logs.sql")) {
                continue; // the one documented purge migration
            }
            String text = name.endsWith(".sql") ? stripSqlComments(Files.readString(file)) : Files.readString(file);
            if (mutate.matcher(text).find()) {
                hits.add(name);
            }
        }
        assertEquals(List.of(), hits);

        for (var method : SystemLogRepository.class.getDeclaredMethods()) {
            String n = method.getName().toLowerCase(Locale.ROOT);
            assertTrue(!n.startsWith("delete") && !n.startsWith("remove") && !n.startsWith("update"), n);
            assertTrue(!method.isAnnotationPresent(Modifying.class), n);
        }
    }

    /** Guards the hand-written H2 mirror used by the repository tests against drift from schema.sql. */
    @Test
    void repositoryH2SchemaIsASubsetOfSchemaSql() throws Exception {
        String schema = Files.readString(Path.of(SCHEMA));
        String h2 = Files.readString(Path.of("src/test/resources/repository-h2-schema.sql"));
        Set<String> real = tableNames(schema);
        List<String> drift = new ArrayList<>();
        Set<String> mirrored = tableNames(h2);
        assertTrue(mirrored.size() >= 10);
        for (String table : mirrored) {
            if (!real.contains(table)) {
                drift.add("unknown table " + table);
                continue;
            }
            Set<String> extra = new TreeSet<>(columnsOf(tableBlock(h2, table)));
            extra.removeAll(columnsOf(tableBlock(schema, table)));
            if (!extra.isEmpty()) {
                drift.add(table + " has columns not in schema.sql: " + extra);
            }
        }
        assertEquals(List.of(), drift);
    }

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

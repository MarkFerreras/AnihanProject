# Course Auto-Create, Custom Combobox & Student-Number Guard — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The Registrar can create a course just by typing its name in the Create Section modal (code auto-derived, e.g. "Culinary Arts and Restaurant Services" → `CARS`); the section's Batch field auto-creates batches the same way; every native `<datalist>` popup is replaced by one shared, styled combobox; and a clashing student number is flagged inline before Save.

**Architecture:** Backend — a pure `CourseCodeGenerator` derives codes; `ClassManagementService.createSection` resolves-or-creates the batch and course inside its existing transaction and returns a `SectionCreationResult` so the controller can audit-log each auto-creation; a read-only availability endpoint reuses the exact holder lookup `assignStudentNumber` already uses. Frontend — one dependency-free `js/combobox.js` (`SrmsCombobox.attach(input, opts)`) replaces all 5 datalists; one `js/student-number-check.js` adds the debounced inline warning to both Assign Student Number modals. **No schema change / no SQL migration** (courses table already has `course_code` PK + `course_name`).

**Tech Stack:** Java 25, Spring Boot 4.0.4, Spring Data JPA, JUnit 5 + Mockito + AssertJ, MockMvc (`@WebMvcTest`), H2 (`@DataJpaTest`), vanilla JS + jQuery 4 + Bootstrap 5.3.

---

## Confirmed decisions (user, 2026-09-30)

| # | Decision |
|---|---|
| 1 | Course is typed by **name**; code is auto-derived from initials of significant words, numeric suffix on collision (`CARS`, `CARS2`…). The derived code is previewed under the field before saving. |
| 2 | Auto-create of courses happens **only in the Create Section modal**. The student edit form's Course field still rejects unknown courses (unchanged backend). |
| 3 | Custom combobox replaces **all** datalists: Assign Batch modal, section Batch + Course, student edit form Course/Section/Sex/Civil Status, Documents upload student picker. The section **Batch** field also becomes type-to-create. |
| 4 | Student-number uniqueness already holds (DB `uq_student_number`, `RegistrarService.assignStudentNumber` pre-check, import `CONFLICT_IN_USE`). Plan **pins it with regression tests** (incl. real-H2 "freed after clear / change / delete") and adds an **inline "already assigned to X" warning** that disables Save. No format-normalisation changes. |

**Input matching rule for `course`:** trimmed input → existing course by **code** (`findById`, `_ci` collation) → else existing course by **name ignoring case** → else **create** `new Course(generatedCode, input)`. The combobox sends the course *name* as the value.

## Branch

Project rule (`.agents/rules/full-stack-anihan.md`, Phase 0) forbids committing code to `main`. The plan document itself was written on `main` (docs-only, same precedent as 2026-09-27). **Task 0 creates `feature/course-auto-create-combobox`; all code commits go there.** If the user explicitly overrides this at execution time, skip Task 0.

## Token-efficiency rules for executors (read once)

- Every task lists the exact files, anchors, and full code. **Do not explore the codebase beyond the files a task names**, and do not read memory-bank logs — the plan is self-contained.
- Run only the test class(es) named in each task. The full suite runs **once**, in Task 8.
- Suggested dispatch grouping (4 Sonnet implementers instead of 9): **A** = Tasks 0–3, **B** = Task 4, **C** = Tasks 5–7, **D** = Task 8. One review after A and one after C (ECC `everything-claude-code:java-reviewer` agent for A/B, `everything-claude-code:code-reviewer` for C).
- Skills: backend tasks follow `superpowers:test-driven-development` (ECC `everything-claude-code:springboot-tdd` for Spring test idioms); Task 8 uses `superpowers:verification-before-completion`, Playwright MCP for the live check, then `superpowers:finishing-a-development-branch`.
- Commits: **no `Co-Authored-By` trailer** (user preference — commits are attributed to Mark only).
- Shell: Git Bash on Windows; use `./gradlew` from the repo root.

## File map

| File | Change | Responsibility |
|---|---|---|
| `service/CourseCodeGenerator.java` | Create | Pure: name → base code; unique code given an `isTaken` predicate |
| `service/SectionCreationResult.java` | Create | `createSection` return: section + `courseCreated` + `batchCreated` flags |
| `dto/registrar/CourseCodePreview.java` | Create | `{code, existing}` for the preview endpoint |
| `dto/registrar/StudentNumberAvailability.java` | Create | `{available, assignedTo}` |
| `dto/registrar/CreateSectionRequest.java` | Modify | `courseCode` → `course`; `@Size` limits matching columns |
| `repository/CourseRepository.java` | Modify | `findFirstByCourseNameIgnoreCase` |
| `service/ClassManagementService.java` | Modify | `createSection` resolve-or-create; `previewCourseCode` |
| `controller/ClassManagementController.java` | Modify | Log auto-creations; `GET /api/registrar/courses/preview-code` |
| `service/RegistrarService.java` | Modify | Extract `findOtherHolder`; `checkStudentNumberAvailability` |
| `controller/RegistrarController.java` | Modify | `GET /api/registrar/student-records/{id}/student-number/availability` |
| `static/js/combobox.js` | Create | `SrmsCombobox` — the one custom dropdown |
| `static/js/student-number-check.js` | Create | `SrmsStudentNumberCheck` — debounced inline clash warning |
| `static/css/dashboard.css` | Modify (append) | Combobox styles |
| `static/registrar.html`, `js/registrar-students.js` | Modify | Assign Batch combobox; number check |
| `static/sections.html`, `js/registrar-sections.js` | Modify | Batch + Course comboboxes, course code preview |
| `static/student-records.html`, `js/registrar-student-records-edit.js` | Modify | 4 comboboxes |
| `static/documents.html`, `js/registrar-documents.js` | Modify | Student picker combobox |
| `static/student-numbers.html`, `js/registrar-student-numbers.js` | Modify | Number check |
| Tests (5 files) | Create/Modify | See tasks |

All Java paths are under `src/main/java/com/example/springboot/`; tests under `src/test/java/com/example/springboot/`; static under `src/main/resources/static/`.

---

### Task 0: Feature branch

- [ ] **Step 1:** Confirm a clean tree and branch off `main`.

```bash
git status --short
git switch -c feature/course-auto-create-combobox
git branch --show-current
```
Expected: no output from `status`; last line `feature/course-auto-create-combobox`.

---

### Task 1: `CourseCodeGenerator` (pure)

**Files:**
- Create: `src/main/java/com/example/springboot/service/CourseCodeGenerator.java`
- Test: `src/test/java/com/example/springboot/service/CourseCodeGeneratorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.springboot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CourseCodeGeneratorTest {

    @Test
    void usesInitialsOfSignificantWords() {
        assertThat(CourseCodeGenerator.baseCode("Culinary Arts and Restaurant Services")).isEqualTo("CARS");
    }

    @Test
    void punctuationSeparatesWords() {
        assertThat(CourseCodeGenerator.baseCode("Food & Beverage Services")).isEqualTo("FBS");
    }

    @Test
    void stopWordsAreSkippedCaseInsensitively() {
        assertThat(CourseCodeGenerator.baseCode("Bread AND Pastry Production")).isEqualTo("BPP");
    }

    @Test
    void digitsAreKept() {
        assertThat(CourseCodeGenerator.baseCode("Cookery NC 2")).isEqualTo("CN2");
    }

    @Test
    void singleWordFallsBackToTheWholeWordUppercased() {
        assertThat(CourseCodeGenerator.baseCode("  Cookery ")).isEqualTo("COOKERY");
    }

    @Test
    void longCodesAreTruncatedToLeaveRoomForASuffix() {
        String name = "Alpha Bravo Charlie Delta Echo Foxtrot Golf Hotel India Juliet "
                + "Kilo Lima Mike November Oscar Papa Quebec Romeo Sierra Tango";
        assertThat(CourseCodeGenerator.baseCode(name)).isEqualTo("ABCDEFGHIJKLMNOPQ");
    }

    @Test
    void blankNameIsRejected() {
        assertThatThrownBy(() -> CourseCodeGenerator.baseCode("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void symbolOnlyNameIsRejected() {
        assertThatThrownBy(() -> CourseCodeGenerator.baseCode("&&&"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uniqueCodeReturnsTheBaseWhenItIsFree() {
        assertThat(CourseCodeGenerator.uniqueCode("Culinary Arts and Restaurant Services", code -> false))
                .isEqualTo("CARS");
    }

    @Test
    void uniqueCodeAppendsTheFirstFreeNumericSuffix() {
        Set<String> taken = Set.of("CARS", "CARS2");
        assertThat(CourseCodeGenerator.uniqueCode("Culinary Arts and Restaurant Services", taken::contains))
                .isEqualTo("CARS3");
    }
}
```

- [ ] **Step 2: Run it — expect compile failure**

Run: `./gradlew test --tests "com.example.springboot.service.CourseCodeGeneratorTest"`
Expected: FAIL — `cannot find symbol: class CourseCodeGenerator`.

- [ ] **Step 3: Implement**

```java
package com.example.springboot.service;

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Derives a {@code courses.course_code} from a course name typed by the registrar, e.g.
 * "Culinary Arts and Restaurant Services" -> "CARS". Pure (no Spring, no DB): the caller
 * supplies an {@code isTaken} predicate so collisions get a numeric suffix (CARS2, CARS3...).
 */
public final class CourseCodeGenerator {

    /** courses.course_code is VARCHAR(20). */
    static final int MAX_CODE_LENGTH = 20;
    /** Leaves room for a suffix of up to 3 digits. */
    private static final int MAX_BASE_LENGTH = MAX_CODE_LENGTH - 3;

    private static final Set<String> STOP_WORDS =
            Set.of("a", "an", "and", "at", "for", "in", "of", "on", "the", "to", "with");

    private CourseCodeGenerator() {
    }

    public static String baseCode(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name is required.");
        }
        String trimmed = courseName.trim();

        StringBuilder initials = new StringBuilder();
        for (String word : trimmed.split("[^\\p{L}\\p{N}]+")) {
            if (word.isEmpty() || STOP_WORDS.contains(word.toLowerCase(Locale.ROOT))) {
                continue;
            }
            initials.append(Character.toUpperCase(word.charAt(0)));
        }

        String code = initials.length() >= 2
                ? initials.toString()
                : trimmed.replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Course name must contain letters or digits.");
        }
        return code.length() > MAX_BASE_LENGTH ? code.substring(0, MAX_BASE_LENGTH) : code;
    }

    public static String uniqueCode(String courseName, Predicate<String> isTaken) {
        String base = baseCode(courseName);
        if (!isTaken.test(base)) {
            return base;
        }
        for (int n = 2; n < 1000; n++) {
            String candidate = base + n;
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique course code for: " + courseName);
    }
}
```

- [ ] **Step 4: Run — expect PASS (10 tests)**

Run: `./gradlew test --tests "com.example.springboot.service.CourseCodeGeneratorTest"`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/springboot/service/CourseCodeGenerator.java src/test/java/com/example/springboot/service/CourseCodeGeneratorTest.java
git commit -m "feat: derive course codes from course names"
```

---

### Task 2: `createSection` resolves-or-creates batch and course; `previewCourseCode`

**Files:**
- Create: `service/SectionCreationResult.java`, `dto/registrar/CourseCodePreview.java`
- Modify: `dto/registrar/CreateSectionRequest.java` (whole file), `repository/CourseRepository.java`, `service/ClassManagementService.java` (imports + `createSection` at ~line 407)
- Test (create): `src/test/java/com/example/springboot/service/ClassManagementSectionCreateServiceTest.java`

Note: no existing test constructs `CreateSectionRequest` (verified), so renaming `courseCode` → `course` breaks nothing but `registrar-sections.js`, which Task 6 updates. Changing `createSection`'s return type breaks `ClassManagementController` until Task 3 — **do Tasks 2 and 3 back-to-back and commit them together.**

- [ ] **Step 1: Write the failing test**

```java
package com.example.springboot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.CourseCodePreview;
import com.example.springboot.dto.registrar.CreateSectionRequest;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Section;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.ClassEnrollmentRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.QualificationRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.SubjectRepository;
import com.example.springboot.repository.UserRepository;

/** Create Section: batch and course are resolved by what the registrar typed, or auto-created. */
@ExtendWith(MockitoExtension.class)
class ClassManagementSectionCreateServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock QualificationRepository qualificationRepository;
    @Mock SchoolClassRepository classRepository;
    @Mock ClassEnrollmentRepository enrollmentRepository;
    @Mock SectionRepository sectionRepository;
    @Mock BatchRepository batchRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserRepository userRepository;
    @Mock StudentRecordRepository studentRecordRepository;

    @InjectMocks ClassManagementService service;

    private final Batch batch = new Batch("B2026A", (short) 2026);
    private final Course cars = new Course("CARS", "Culinary Arts and Restaurant Services");

    @BeforeEach
    void echoSaves() {
        lenient().when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateSectionRequest request(String batchCode, String course) {
        return new CreateSectionRequest("SEC-A", "Section A", batchCode, course);
    }

    @Test
    void usesAnExistingCourseMatchedByCode() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));
        when(courseRepository.findById("CARS")).thenReturn(Optional.of(cars));

        SectionCreationResult result = service.createSection(request("B2026A", "CARS"));

        assertThat(result.section().courseCode()).isEqualTo("CARS");
        assertThat(result.courseCreated()).isFalse();
        assertThat(result.batchCreated()).isFalse();
        verify(courseRepository, never()).save(any());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void usesAnExistingCourseMatchedByNameIgnoringCase() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));
        when(courseRepository.findFirstByCourseNameIgnoreCase("culinary arts and restaurant services"))
                .thenReturn(Optional.of(cars));

        SectionCreationResult result = service.createSection(request("B2026A", "culinary arts and restaurant services"));

        assertThat(result.section().courseCode()).isEqualTo("CARS");
        assertThat(result.courseCreated()).isFalse();
    }

    @Test
    void createsANewCourseWithAGeneratedCodeAndTrimmedName() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));

        SectionCreationResult result =
                service.createSection(request("B2026A", "  Bread and Pastry Production  "));

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(saved.capture());
        assertThat(saved.getValue().getCourseCode()).isEqualTo("BPP");
        assertThat(saved.getValue().getCourseName()).isEqualTo("Bread and Pastry Production");
        assertThat(result.courseCreated()).isTrue();
        assertThat(result.section().courseCode()).isEqualTo("BPP");
    }

    @Test
    void suffixesTheGeneratedCodeWhenItIsAlreadyTaken() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));
        when(courseRepository.existsById("CARS")).thenReturn(true);

        SectionCreationResult result =
                service.createSection(request("B2026A", "Culinary Arts and Retail Sales"));

        assertThat(result.section().courseCode()).isEqualTo("CARS2");
        assertThat(result.courseCreated()).isTrue();
    }

    @Test
    void createsANewBatchWithTheCurrentYear() {
        when(courseRepository.findById("CARS")).thenReturn(Optional.of(cars));

        SectionCreationResult result = service.createSection(request(" B2027A ", "CARS"));

        ArgumentCaptor<Batch> saved = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(saved.capture());
        assertThat(saved.getValue().getBatchCode()).isEqualTo("B2027A");
        assertThat(saved.getValue().getBatchYear()).isEqualTo((short) LocalDate.now().getYear());
        assertThat(result.batchCreated()).isTrue();
    }

    @Test
    void rejectsADuplicateSectionCodeBeforeCreatingAnything() {
        when(sectionRepository.existsById("SEC-A")).thenReturn(true);

        assertThatThrownBy(() -> service.createSection(request("B2027A", "Brand New Course")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section code already exists");
        verify(batchRepository, never()).save(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void previewReturnsTheExistingCodeForAKnownName() {
        when(courseRepository.findFirstByCourseNameIgnoreCase("Culinary Arts and Restaurant Services"))
                .thenReturn(Optional.of(cars));

        CourseCodePreview preview = service.previewCourseCode(" Culinary Arts and Restaurant Services ");

        assertThat(preview.code()).isEqualTo("CARS");
        assertThat(preview.existing()).isTrue();
    }

    @Test
    void previewGeneratesACodeForANewName() {
        CourseCodePreview preview = service.previewCourseCode("Bread and Pastry Production");

        assertThat(preview.code()).isEqualTo("BPP");
        assertThat(preview.existing()).isFalse();
        verify(courseRepository, never()).save(any());
    }

    @Test
    void previewRejectsABlankName() {
        assertThatThrownBy(() -> service.previewCourseCode("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run — expect compile failure**

Run: `./gradlew test --tests "com.example.springboot.service.ClassManagementSectionCreateServiceTest"`
Expected: FAIL — `SectionCreationResult`, `CourseCodePreview`, `findFirstByCourseNameIgnoreCase`, `previewCourseCode` not found.

- [ ] **Step 3: Create the two records**

`src/main/java/com/example/springboot/service/SectionCreationResult.java`:
```java
package com.example.springboot.service;

import com.example.springboot.dto.registrar.SectionResponse;

/**
 * What {@link ClassManagementService#createSection} did, so the controller can write one
 * audit row per auto-created batch/course in addition to the section row.
 */
public record SectionCreationResult(SectionResponse section, boolean courseCreated, boolean batchCreated) {
}
```

`src/main/java/com/example/springboot/dto/registrar/CourseCodePreview.java`:
```java
package com.example.springboot.dto.registrar;

/** Code a typed course name resolves to: an existing course's code, or the one that would be generated. */
public record CourseCodePreview(String code, boolean existing) {
}
```

- [ ] **Step 4: Replace `CreateSectionRequest.java` entirely**

```java
package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create Section payload. {@code batchCode} and {@code course} are free text: an unknown batch
 * code or course name is auto-created by ClassManagementService.createSection. {@code course}
 * may be an existing course code or name; sizes mirror the schema columns.
 */
public record CreateSectionRequest(
    @NotBlank @Size(max = 20, message = "Section code must not exceed 20 characters.") String sectionCode,
    @NotBlank @Size(max = 25, message = "Section name must not exceed 25 characters.") String sectionName,
    @NotBlank @Size(max = 20, message = "Batch code must not exceed 20 characters.") String batchCode,
    @NotBlank @Size(max = 100, message = "Course name must not exceed 100 characters.") String course
) {
}
```

- [ ] **Step 5: Add the repository finder** — `repository/CourseRepository.java` becomes:

```java
package com.example.springboot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Course;

public interface CourseRepository extends JpaRepository<Course, String> {

    /** course_name is not unique in the schema, so take the first match. */
    Optional<Course> findFirstByCourseNameIgnoreCase(String courseName);
}
```

- [ ] **Step 6: Update `ClassManagementService`**

Add imports next to the existing `java.*` ones:
```java
import java.time.LocalDate;
import java.util.Optional;
```
and `import com.example.springboot.dto.registrar.CourseCodePreview;` with the other dto imports.

Replace the whole `createSection` method (currently `public SectionResponse createSection(CreateSectionRequest request) { ... }`, ~line 407) with:

```java
    /**
     * Creates a section. The batch code and course are free text: an unknown batch is created
     * with the current calendar year (same rule as RegistrarService.assignBatch), and an
     * unknown course is created with a code derived from its name (CourseCodeGenerator).
     */
    @Transactional
    public SectionCreationResult createSection(CreateSectionRequest request) {
        if (sectionRepository.existsById(request.sectionCode())) {
            throw new IllegalArgumentException("Section code already exists: " + request.sectionCode());
        }

        String batchCode = request.batchCode().trim();
        Optional<Batch> existingBatch = batchRepository.findById(batchCode);
        Batch batch = existingBatch.orElseGet(() ->
                batchRepository.save(new Batch(batchCode, (short) LocalDate.now().getYear())));

        String courseInput = request.course().trim();
        Optional<Course> existingCourse = findCourse(courseInput);
        Course course = existingCourse.orElseGet(() -> courseRepository.save(new Course(
                CourseCodeGenerator.uniqueCode(courseInput, courseRepository::existsById), courseInput)));

        Section section = new Section();
        section.setSectionCode(request.sectionCode());
        section.setSection(request.sectionName());
        section.setBatch(batch);
        section.setCourse(course);

        sectionRepository.save(section);
        return new SectionCreationResult(SectionResponse.from(section),
                existingCourse.isEmpty(), existingBatch.isEmpty());
    }

    /** Read-only: the code a typed course name would resolve to, shown before saving. */
    public CourseCodePreview previewCourseCode(String courseName) {
        String name = courseName == null ? "" : courseName.trim();
        return findCourse(name)
                .map(c -> new CourseCodePreview(c.getCourseCode(), true))
                .orElseGet(() -> new CourseCodePreview(
                        CourseCodeGenerator.uniqueCode(name, courseRepository::existsById), false));
    }

    /** A typed course matches an existing course by code first, then by name (case-insensitive). */
    private Optional<Course> findCourse(String input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return courseRepository.findById(input)
                .or(() -> courseRepository.findFirstByCourseNameIgnoreCase(input));
    }
```

- [ ] **Step 7:** Go straight to Task 3 (main code compiles only once the controller is updated). Task 2's tests are run in Task 3 Step 4.

---

### Task 3: Controller — audit auto-creations; course-code preview endpoint

**Files:**
- Modify: `controller/ClassManagementController.java` (`createSection`, ~line 260; `GetMapping`/`RequestParam` are already imported)
- Test (create): `src/test/java/com/example/springboot/controller/ClassManagementCourseControllerWebMvcTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.CourseCodePreview;
import com.example.springboot.dto.registrar.SectionResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.ClassManagementService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SectionCreationResult;
import com.example.springboot.service.SystemLogService;

@WebMvcTest(ClassManagementController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ClassManagementCourseControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassManagementService classManagementService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private static final SectionResponse CREATED = new SectionResponse(
            "SEC-A", "Section A", "B2027A", (short) 2027, "BPP", "Bread and Pastry Production");
    private static final String BODY = "{\"sectionCode\":\"SEC-A\",\"sectionName\":\"Section A\","
            + "\"batchCode\":\"B2027A\",\"course\":\"Bread and Pastry Production\"}";

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionLogsTheAutoCreatedBatchAndCourse() throws Exception {
        when(classManagementService.createSection(any())).thenReturn(new SectionCreationResult(CREATED, true, true));

        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("BPP"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Created batch: B2027A"), any());
        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Created course: Bread and Pastry Production (BPP)"), any());
        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Created section"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionWithExistingBatchAndCourseLogsOnlyTheSection() throws Exception {
        when(classManagementService.createSection(any())).thenReturn(new SectionCreationResult(CREATED, false, false));

        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY).with(csrf()))
                .andExpect(status().isCreated());

        verify(systemLogService, times(1)).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionRejectsAMissingCourse() throws Exception {
        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionCode\":\"SEC-A\",\"sectionName\":\"Section A\",\"batchCode\":\"B2027A\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
        verify(classManagementService, never()).createSection(any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionRejectsASectionNameLongerThanTheColumn() throws Exception {
        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionCode\":\"SEC-A\",\"sectionName\":\"Section A Morning Batch 2027 X\","
                                + "\"batchCode\":\"B2027A\",\"course\":\"CARS\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void previewCourseCodeReturnsTheCode() throws Exception {
        when(classManagementService.previewCourseCode("Bread and Pastry Production"))
                .thenReturn(new CourseCodePreview("BPP", false));

        mvc.perform(get("/api/registrar/courses/preview-code").param("name", "Bread and Pastry Production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BPP"))
                .andExpect(jsonPath("$.existing").value(false));
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = {"TRAINER"})
    void trainerCannotPreviewCourseCodes() throws Exception {
        mvc.perform(get("/api/registrar/courses/preview-code").param("name", "X"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run — expect FAIL** (compile error: `createSection` return type / `previewCourseCode` missing in controller)

Run: `./gradlew test --tests "com.example.springboot.controller.ClassManagementCourseControllerWebMvcTest"`

- [ ] **Step 3: Implement** — replace the `createSection` handler in `ClassManagementController` and add the preview handler right after it. Add imports `com.example.springboot.dto.registrar.CourseCodePreview` and `com.example.springboot.service.SectionCreationResult`.

```java
    @PostMapping("/sections")
    public ResponseEntity<SectionResponse> createSection(
            @Valid @RequestBody CreateSectionRequest request,
            HttpServletRequest httpRequest) {
        SectionCreationResult result = classManagementService.createSection(request);
        SectionResponse section = result.section();

        LogContext ctx = getLogContext();
        String ip = httpRequest.getRemoteAddr();
        if (result.batchCreated()) {
            systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                    "Created batch: " + section.batchCode(), ip);
        }
        if (result.courseCreated()) {
            systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                    "Created course: " + section.courseName() + " (" + section.courseCode() + ")", ip);
        }
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Created section: " + section.sectionName() + " (" + section.sectionCode() + ")", ip);

        return ResponseEntity.status(HttpStatus.CREATED).body(section);
    }

    /** Read-only preview behind the Create Section modal's "code will be ..." hint. */
    @GetMapping("/courses/preview-code")
    public ResponseEntity<CourseCodePreview> previewCourseCode(@RequestParam String name) {
        return ResponseEntity.ok(classManagementService.previewCourseCode(name));
    }
```

- [ ] **Step 4: Run — expect PASS** (Tasks 2 + 3 + the existing section controller tests)

Run: `./gradlew test --tests "com.example.springboot.controller.ClassManagementCourseControllerWebMvcTest" --tests "com.example.springboot.controller.ClassManagementSectionControllerWebMvcTest" --tests "com.example.springboot.service.ClassManagementSectionCreateServiceTest"`

- [ ] **Step 5: Commit (Tasks 2 + 3 together)**

```bash
git add src/main/java/com/example/springboot/service/SectionCreationResult.java src/main/java/com/example/springboot/dto/registrar/CourseCodePreview.java src/main/java/com/example/springboot/dto/registrar/CreateSectionRequest.java src/main/java/com/example/springboot/repository/CourseRepository.java src/main/java/com/example/springboot/service/ClassManagementService.java src/main/java/com/example/springboot/controller/ClassManagementController.java src/test/java/com/example/springboot/service/ClassManagementSectionCreateServiceTest.java src/test/java/com/example/springboot/controller/ClassManagementCourseControllerWebMvcTest.java
git commit -m "feat: auto-create batch and course when creating a section, with audit logs and code preview"
```

---

### Task 4: Student-number availability check + uniqueness regression tests

**Files:**
- Create: `dto/registrar/StudentNumberAvailability.java`
- Modify: `service/RegistrarService.java` (`assignStudentNumber`, ~line 175), `controller/RegistrarController.java` (after `assignStudentNumber` handler, ~line 110)
- Modify tests: `service/RegistrarStudentNumberServiceTest.java`, `controller/RegistrarStudentNumberControllerWebMvcTest.java`
- Create test: `src/test/java/com/example/springboot/integration/StudentNumberUniquenessH2Test.java`

- [ ] **Step 1: Write the failing unit tests** — append inside `RegistrarStudentNumberServiceTest` (before the final `}`), and add imports `static org.junit.jupiter.api.Assertions.assertFalse` and `com.example.springboot.dto.registrar.StudentNumberAvailability`:

```java
    // ----- Availability check (inline warning before Save) -----

    @Test
    void availabilityNamesTheHolderWhenAnotherStudentHasTheNumber() {
        StudentRecord holder = record(2, "SR20260002", "2026-001");
        holder.setLastName("Santos");
        holder.setFirstName("Ana");
        when(studentRecordRepository.findByStudentNumber("2026-001")).thenReturn(Optional.of(holder));

        StudentNumberAvailability result = registrarService.checkStudentNumberAvailability(1, " 2026-001 ");

        assertFalse(result.available());
        assertEquals("Santos, Ana", result.assignedTo());
    }

    @Test
    void availabilityIsTrueWhenTheOnlyHolderIsTheSameStudent() {
        when(studentRecordRepository.findByStudentNumber("2026-001"))
                .thenReturn(Optional.of(record(1, "SR20260001", "2026-001")));

        StudentNumberAvailability result = registrarService.checkStudentNumberAvailability(1, "2026-001");

        assertTrue(result.available());
        assertNull(result.assignedTo());
    }

    @Test
    void availabilityIsTrueWhenNobodyHasTheNumber() {
        when(studentRecordRepository.findByStudentNumber("2026-009")).thenReturn(Optional.empty());

        assertTrue(registrarService.checkStudentNumberAvailability(1, "2026-009").available());
    }

    @Test
    void aBlankNumberIsAlwaysAvailableAndNeverQueried() {
        assertTrue(registrarService.checkStudentNumberAvailability(1, "   ").available());
        verify(studentRecordRepository, never()).findByStudentNumber(any());
    }
```

Append inside `RegistrarStudentNumberControllerWebMvcTest`, adding imports `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get` and `com.example.springboot.dto.registrar.StudentNumberAvailability`:

```java
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void availabilityReportsTheHolderWithoutLogging() throws Exception {
        when(registrarService.checkStudentNumberAvailability(1, "2026-001"))
                .thenReturn(new StudentNumberAvailability(false, "Santos, Ana"));

        mvc.perform(get("/api/registrar/student-records/1/student-number/availability")
                        .param("number", "2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.assignedTo").value("Santos, Ana"));
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotCheckAvailability() throws Exception {
        mvc.perform(get("/api/registrar/student-records/1/student-number/availability")
                        .param("number", "2026-001"))
                .andExpect(status().isForbidden());
    }
```

- [ ] **Step 2: Run — expect compile FAIL**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarStudentNumberServiceTest" --tests "com.example.springboot.controller.RegistrarStudentNumberControllerWebMvcTest"`

- [ ] **Step 3: Implement**

`src/main/java/com/example/springboot/dto/registrar/StudentNumberAvailability.java`:
```java
package com.example.springboot.dto.registrar;

/** Whether a student number is free for a record; {@code assignedTo} is "Last, First" of the other holder, else null. */
public record StudentNumberAvailability(boolean available, String assignedTo) {
}
```

In `RegistrarService`, add import `com.example.springboot.dto.registrar.StudentNumberAvailability` (and `java.util.Optional` if not already imported). Replace the `if (normalized != null) { studentRecordRepository.findByStudentNumber(...)...ifPresent(...) }` block inside `assignStudentNumber` with:

```java
        if (normalized != null) {
            findOtherHolder(recordId, normalized).ifPresent(other -> {
                throw new IllegalArgumentException(
                        "Student number " + normalized + " is already assigned to "
                                + other.getLastName() + ", " + other.getFirstName() + ".");
            });
        }
```
and add directly after `assignStudentNumber`:

```java
    /**
     * Read-only pre-check for the Assign Student Number modal's inline warning. Uses the same
     * lookup as {@link #assignStudentNumber}, which remains the authority on save.
     */
    public StudentNumberAvailability checkStudentNumberAvailability(Integer recordId, String studentNumber) {
        String normalized = emptyToNull(studentNumber);
        if (normalized == null) {
            return new StudentNumberAvailability(true, null);
        }
        return findOtherHolder(recordId, normalized)
                .map(other -> new StudentNumberAvailability(false,
                        other.getLastName() + ", " + other.getFirstName()))
                .orElseGet(() -> new StudentNumberAvailability(true, null));
    }

    /** The student (other than {@code recordId}) currently holding {@code normalized}, if any. */
    private Optional<StudentRecord> findOtherHolder(Integer recordId, String normalized) {
        return studentRecordRepository.findByStudentNumber(normalized)
                .filter(other -> !other.getRecordId().equals(recordId));
    }
```

In `RegistrarController` add import `com.example.springboot.dto.registrar.StudentNumberAvailability` and, after the `assignStudentNumber` handler:

```java
    /**
     * Read-only pre-check behind the Assign Student Number modal's inline warning. The PUT
     * above stays the authority; this only surfaces a clash before Save. Not audit-logged.
     */
    @GetMapping("/{recordId}/student-number/availability")
    public ResponseEntity<StudentNumberAvailability> checkStudentNumberAvailability(
            @PathVariable Integer recordId,
            @RequestParam(name = "number", required = false) String number) {
        return ResponseEntity.ok(registrarService.checkStudentNumberAvailability(recordId, number));
    }
```

- [ ] **Step 4: Run — expect PASS** (same command as Step 2; the pre-existing `rejectsANumberAlreadyAssignedToAnotherStudent` must still pass)

- [ ] **Step 5: Add the real-database regression test** — `src/test/java/com/example/springboot/integration/StudentNumberUniquenessH2Test.java`. This pins behaviour that already exists, so it should **pass on first run**; a failure means a real regression.

```java
package com.example.springboot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.service.RegistrarService;

/**
 * Real-JPA guarantee that two students can never share a student number, and that a number
 * becomes reusable only once its holder's number is cleared, changed, or the holder deleted.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@Import(RegistrarService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:studentNumberUniqueDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StudentNumberUniquenessH2Test {

    @Autowired private RegistrarService registrarService;
    @Autowired private StudentRecordRepository studentRecordRepository;

    private Integer newStudent(String studentId, String lastName) {
        StudentRecord r = new StudentRecord();
        r.setStudentId(studentId);
        r.setLastName(lastName);
        r.setFirstName("Test");
        r.setMiddleName("H2");
        r.setBaptized(false);
        r.setStudentStatus("Active");
        return studentRecordRepository.saveAndFlush(r).getRecordId();
    }

    @Test
    void aTakenNumberIsRejectedUntilItsHolderClearsIt() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        registrarService.assignStudentNumber(ana, "2026-001");

        IllegalArgumentException clash = assertThrows(IllegalArgumentException.class,
                () -> registrarService.assignStudentNumber(bea, "2026-001"));
        assertTrue(clash.getMessage().contains("already assigned to Santos"));

        registrarService.assignStudentNumber(ana, "");
        assertEquals("2026-001", registrarService.assignStudentNumber(bea, "2026-001").studentNumber());
    }

    @Test
    void aNumberIsFreedWhenItsHolderIsChangedToAnotherNumber() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        registrarService.assignStudentNumber(ana, "2026-001");
        registrarService.assignStudentNumber(ana, "2026-002");

        assertEquals("2026-001", registrarService.assignStudentNumber(bea, "2026-001").studentNumber());
    }

    @Test
    void aNumberIsFreedWhenItsHolderIsDeleted() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        registrarService.assignStudentNumber(ana, "2026-001");
        studentRecordRepository.deleteById(ana);
        studentRecordRepository.flush();

        assertEquals("2026-001", registrarService.assignStudentNumber(bea, "2026-001").studentNumber());
    }

    @Test
    void theUniqueIndexRejectsADuplicateThatBypassesTheService() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        StudentRecord a = studentRecordRepository.findById(ana).orElseThrow();
        a.setStudentNumber("2026-001");
        studentRecordRepository.saveAndFlush(a);

        StudentRecord b = studentRecordRepository.findById(bea).orElseThrow();
        b.setStudentNumber("2026-001");
        assertThrows(DataIntegrityViolationException.class, () -> studentRecordRepository.saveAndFlush(b));
    }
}
```

- [ ] **Step 6: Run — expect PASS (4 tests)**

Run: `./gradlew test --tests "com.example.springboot.integration.StudentNumberUniquenessH2Test"`
If setup fails on a missing non-null column, set that field in `newStudent` (compare `StudentRecordH2LoadTest.buildDummyRecords`) — do not change production code.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/springboot/dto/registrar/StudentNumberAvailability.java src/main/java/com/example/springboot/service/RegistrarService.java src/main/java/com/example/springboot/controller/RegistrarController.java src/test/java/com/example/springboot/service/RegistrarStudentNumberServiceTest.java src/test/java/com/example/springboot/controller/RegistrarStudentNumberControllerWebMvcTest.java src/test/java/com/example/springboot/integration/StudentNumberUniquenessH2Test.java
git commit -m "feat: student number availability check and uniqueness regression tests"
```

---

### Task 5: Shared frontend components — `SrmsCombobox`, `SrmsStudentNumberCheck`, CSS

**Files:**
- Create: `src/main/resources/static/js/combobox.js`, `src/main/resources/static/js/student-number-check.js`
- Modify: `src/main/resources/static/css/dashboard.css` (append at end)

No JS test harness exists in this repo; these are verified live in Task 8.

- [ ] **Step 1: Create `js/combobox.js`**

```js
/*
 * SrmsCombobox — type-to-filter dropdown that replaces the browser's native <datalist> popup.
 *
 *   const cb = SrmsCombobox.attach(inputEl, {
 *       items: [{ value: 'B2026A', label: 'B2026A', hint: '2026' }],  // optional initial items
 *       emptyText: 'No matches',                                        // shown when nothing matches
 *       onSelect: function (item) {}                                    // optional, after a pick
 *   });
 *   cb.setItems(newItems);
 *
 * Free typing stays allowed (the input's value is what gets submitted). Picking an option sets
 * input.value = item.value and fires a bubbling 'input' event so existing listeners still run.
 * Keyboard: ArrowUp/ArrowDown move, Enter picks, Escape closes (without closing a Bootstrap modal).
 */
(function () {
    'use strict';

    const MAX_RESULTS = 50;
    let idCounter = 0;

    function attach(input, options) {
        const opts = Object.assign({ items: [], emptyText: 'No matches', onSelect: null }, options || {});
        let items = opts.items.slice();
        let visible = [];
        let activeIndex = -1;
        let selecting = false;

        input.removeAttribute('list');
        input.setAttribute('autocomplete', 'off');
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-autocomplete', 'list');
        input.setAttribute('aria-expanded', 'false');

        const wrapper = document.createElement('div');
        wrapper.className = 'srms-combobox';
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);

        const menu = document.createElement('ul');
        menu.id = 'srmsCombobox' + (++idCounter);
        menu.className = 'dropdown-menu srms-combobox-menu';
        menu.setAttribute('role', 'listbox');
        wrapper.appendChild(menu);
        input.setAttribute('aria-controls', menu.id);

        function matches(item, query) {
            if (!query) return true;
            const haystack = (item.value + ' ' + (item.label || '') + ' ' + (item.hint || '')).toLowerCase();
            return haystack.indexOf(query) !== -1;
        }

        function render() {
            const query = input.value.trim().toLowerCase();
            visible = items.filter(function (item) { return matches(item, query); }).slice(0, MAX_RESULTS);
            menu.innerHTML = '';

            if (visible.length === 0) {
                const empty = document.createElement('li');
                empty.className = 'dropdown-item-text text-muted small';
                empty.textContent = opts.emptyText;
                menu.appendChild(empty);
            }

            visible.forEach(function (item, i) {
                const li = document.createElement('li');
                li.id = menu.id + '-opt' + i;
                li.className = 'dropdown-item srms-combobox-option' + (i === activeIndex ? ' active' : '');
                li.setAttribute('role', 'option');
                li.setAttribute('aria-selected', i === activeIndex ? 'true' : 'false');

                const main = document.createElement('span');
                main.textContent = item.label || item.value;
                li.appendChild(main);
                if (item.hint) {
                    const hint = document.createElement('span');
                    hint.className = 'srms-combobox-hint';
                    hint.textContent = item.hint;
                    li.appendChild(hint);
                }

                // mousedown + preventDefault keeps focus in the input, so blur doesn't close first.
                li.addEventListener('mousedown', function (e) {
                    e.preventDefault();
                    choose(i);
                });
                menu.appendChild(li);
            });

            if (activeIndex >= 0) {
                input.setAttribute('aria-activedescendant', menu.id + '-opt' + activeIndex);
                const activeEl = document.getElementById(menu.id + '-opt' + activeIndex);
                if (activeEl) activeEl.scrollIntoView({ block: 'nearest' });
            } else {
                input.removeAttribute('aria-activedescendant');
            }
        }

        function isOpen() {
            return menu.classList.contains('show');
        }

        function open() {
            if (input.readOnly || input.disabled) return;
            render();
            menu.classList.add('show');
            input.setAttribute('aria-expanded', 'true');
        }

        function close() {
            menu.classList.remove('show');
            input.setAttribute('aria-expanded', 'false');
            input.removeAttribute('aria-activedescendant');
            activeIndex = -1;
        }

        function choose(i) {
            const item = visible[i];
            if (!item) return;
            input.value = item.value;
            close();
            selecting = true;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            selecting = false;
            if (opts.onSelect) opts.onSelect(item);
        }

        input.addEventListener('focus', open);
        input.addEventListener('click', function () { if (!isOpen()) open(); });
        input.addEventListener('blur', close);
        input.addEventListener('input', function () {
            if (selecting) return;
            activeIndex = -1;
            open();
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                if (!isOpen()) open();
                activeIndex = Math.min(activeIndex + 1, visible.length - 1);
                render();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                if (isOpen() && visible.length > 0) {
                    activeIndex = Math.max(activeIndex - 1, 0);
                    render();
                }
            } else if (e.key === 'Enter') {
                if (isOpen() && activeIndex >= 0) {
                    e.preventDefault();
                    choose(activeIndex);
                }
            } else if (e.key === 'Escape') {
                if (isOpen()) {
                    e.preventDefault();
                    e.stopPropagation(); // don't let Bootstrap close the surrounding modal
                    close();
                }
            } else if (e.key === 'Tab') {
                close();
            }
        });

        return {
            setItems: function (newItems) {
                items = (newItems || []).slice();
                if (isOpen()) render();
            },
            close: close
        };
    }

    window.SrmsCombobox = { attach: attach };
})();
```

- [ ] **Step 2: Create `js/student-number-check.js`**

```js
/*
 * SrmsStudentNumberCheck — while the registrar types in an Assign Student Number modal, warns
 * (and disables Save) if another student already holds that number. The PUT on Save remains
 * the authority; this only surfaces the clash earlier.
 *
 *   const check = SrmsStudentNumberCheck.attach({ input, saveBtn, getRecordId: () => id });
 *   check.reset();   // call when the modal opens
 */
(function () {
    'use strict';

    const DEBOUNCE_MS = 300;

    function attach(opts) {
        const input = opts.input;
        const saveBtn = opts.saveBtn;
        let timer = null;
        let seq = 0;

        const feedback = document.createElement('div');
        feedback.className = 'invalid-feedback';
        input.insertAdjacentElement('afterend', feedback);

        function reset() {
            window.clearTimeout(timer);
            seq++;
            input.classList.remove('is-invalid');
            feedback.textContent = '';
            saveBtn.disabled = false;
        }

        input.addEventListener('input', function () {
            reset();
            const value = input.value.trim();
            const recordId = opts.getRecordId();
            if (!value || !recordId) return;

            const mySeq = seq;
            timer = window.setTimeout(async function () {
                try {
                    const res = await fetch('/api/registrar/student-records/' + encodeURIComponent(recordId)
                            + '/student-number/availability?number=' + encodeURIComponent(value),
                        { credentials: 'same-origin' });
                    if (!res.ok || mySeq !== seq) return;
                    const body = await res.json();
                    if (mySeq !== seq || body.available) return;
                    input.classList.add('is-invalid');
                    feedback.textContent = 'Student number ' + value + ' is already assigned to '
                        + body.assignedTo + '.';
                    saveBtn.disabled = true;
                } catch (err) {
                    // Network hiccup: no inline warning; the server still rejects a clash on Save.
                }
            }, DEBOUNCE_MS);
        });

        return { reset: reset };
    }

    window.SrmsStudentNumberCheck = { attach: attach };
})();
```

- [ ] **Step 3: Append to `css/dashboard.css`**

```css

/* ===== SrmsCombobox (js/combobox.js) — replaces native <datalist> popups ===== */
.srms-combobox {
    position: relative;
}

.srms-combobox-menu {
    top: 100%;
    left: 0;
    width: 100%;
    max-height: 16rem;
    overflow-y: auto;
    margin-top: 2px;
}

.srms-combobox-option {
    display: flex;
    justify-content: space-between;
    gap: 0.75rem;
    cursor: pointer;
    white-space: normal;
}

.srms-combobox-hint {
    flex-shrink: 0;
    color: var(--bs-secondary-color);
    font-size: 0.8125rem;
}

.srms-combobox-option.active .srms-combobox-hint {
    color: inherit;
    opacity: 0.85;
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/js/combobox.js src/main/resources/static/js/student-number-check.js src/main/resources/static/css/dashboard.css
git commit -m "feat: shared custom combobox and student number check components"
```

---

### Task 6: Wire comboboxes — Assign Batch modal and Create Section modal

**Files:** `static/registrar.html`, `static/js/registrar-students.js`, `static/sections.html`, `static/js/registrar-sections.js`

- [ ] **Step 1: `registrar.html`** — in `#assignBatchModal` replace the input + datalist with:

```html
                        <input type="text" class="form-control" id="assignBatchInput"
                               maxlength="20" autocomplete="off" spellcheck="false"
                               placeholder="Type or pick a batch code (e.g. B2026A)">
```
(the `<datalist id="batchLookupList"></datalist>` line is deleted). Change `css/dashboard.css?v=3` → `?v=4`. Replace `<script src="js/registrar-students.js?v=8"></script>` with:

```html
    <script src="js/combobox.js?v=1"></script>
    <script src="js/student-number-check.js?v=1"></script>
    <script src="js/registrar-students.js?v=9"></script>
```

- [ ] **Step 2: `js/registrar-students.js` — batch combobox.** Add `let batchCombobox = null;` next to `let batchLookupLoaded = false;`. Replace `loadBatchLookupOptions` with:

```js
    async function loadBatchLookupOptions() {
        try {
            const response = await fetch('/api/lookup/batches', { credentials: 'same-origin' });
            if (!response.ok || !batchCombobox) return;
            const items = await response.json();
            batchCombobox.setItems(items.map(function (item) {
                return { value: item.code, label: item.code, hint: item.name };
            }));
            batchLookupLoaded = true;
        } catch (error) {
            // List stays empty; registrar can still type a free-text code.
        }
    }
```
In `setupAssignBatch`, right after `assignBatchModal = new bootstrap.Modal(modalEl);`:

```js
        batchCombobox = SrmsCombobox.attach(inputEl, {
            emptyText: 'No existing batch matches — saving will create it.'
        });
```
In `save()`'s success path, directly after `const saved = await res.json();`, add `batchLookupLoaded = false;` so a newly created batch appears the next time the modal opens (fixes the 2026-09-27 stale-datalist observation).

- [ ] **Step 3: `js/registrar-students.js` — number check.** Add `let numberCheck = null;` next to `let assignNumberModal = null;`. In `setupAssignStudentNumber`, after `assignNumberModal = new bootstrap.Modal(modalEl);`:

```js
        numberCheck = SrmsStudentNumberCheck.attach({
            input: inputEl,
            saveBtn: saveBtn,
            getRecordId: function () { return assignTargetRecordId; }
        });
```
In `openAssignNumberModal`, just before `assignNumberModal.show();`: `if (numberCheck) numberCheck.reset();`

- [ ] **Step 4: `sections.html`** — replace the Batch and Course `<div class="mb-3">` blocks of `#createSectionModal` (the two `<select>`s) with:

```html
                    <div class="mb-3">
                        <label for="sectionBatchInput" class="form-label">Batch *</label>
                        <input type="text" class="form-control" id="sectionBatchInput" maxlength="20"
                               autocomplete="off" spellcheck="false" required
                               placeholder="Type or pick a batch code (e.g. B2026A)">
                        <div class="form-text">Typing a batch code that doesn't exist yet creates that batch.</div>
                    </div>
                    <div class="mb-3">
                        <label for="sectionCourseInput" class="form-label">Course *</label>
                        <input type="text" class="form-control" id="sectionCourseInput" maxlength="100"
                               autocomplete="off" required placeholder="Type or pick a course name">
                        <div class="form-text" id="sectionCourseHelp">Typing a course name that doesn't exist yet creates that course.</div>
                    </div>
```
Also add `maxlength="20"` to `#sectionCodeInput` and `maxlength="25"` to `#sectionNameInput`. Change `css/dashboard.css?v=3` → `?v=4`; replace the page script tag with:

```html
    <script src="js/combobox.js?v=1"></script>
    <script src="js/registrar-sections.js?v=4"></script>
```

- [ ] **Step 5: `js/registrar-sections.js`** — replace everything from `function setupCreateSection() {` through the end of `function loadCoursesDropdown() { ... }` (just before the `// Edit Section` banner) with:

```js
    const DEFAULT_COURSE_HELP = "Typing a course name that doesn't exist yet creates that course.";
    let sectionBatchCombobox = null;
    let sectionCourseCombobox = null;
    let coursePreviewTimer = null;

    function setupCreateSection() {
        sectionBatchCombobox = SrmsCombobox.attach(document.getElementById('sectionBatchInput'), {
            emptyText: 'No existing batch matches — saving will create it.'
        });
        sectionCourseCombobox = SrmsCombobox.attach(document.getElementById('sectionCourseInput'), {
            emptyText: 'No existing course matches — saving will create it.'
        });
        $('#sectionCourseInput').on('input', scheduleCoursePreview);

        $('#createSectionModal').on('show.bs.modal', function () {
            hideAlert('createSectionAlert');
            $('#sectionCodeInput, #sectionNameInput, #sectionBatchInput, #sectionCourseInput').val('');
            $('#sectionCourseHelp').text(DEFAULT_COURSE_HELP);
            loadBatchesDropdown();
            loadCoursesDropdown();
        });

        $('#saveSectionBtn').on('click', function () {
            const payload = {
                sectionCode: $('#sectionCodeInput').val().trim(),
                sectionName: $('#sectionNameInput').val().trim(),
                batchCode: $('#sectionBatchInput').val().trim(),
                course: $('#sectionCourseInput').val().trim()
            };

            if (!payload.sectionCode || !payload.sectionName || !payload.batchCode || !payload.course) {
                showAlert('createSectionAlert', 'Please fill in all required fields.', 'danger');
                return;
            }

            $.ajax({
                url: '/api/registrar/sections',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function () {
                    createSectionModal.hide();
                    sectionsTable.ajax.reload(null, false);
                    loadFilterDropdowns(); // a new batch/course must appear in the eligible-student filters
                },
                error: function (xhr) {
                    const body = xhr.responseJSON || {};
                    const fieldErrors = body.errors ? Object.values(body.errors).join(' ') : '';
                    showAlert('createSectionAlert', fieldErrors || body.message || 'Failed to create section.', 'danger');
                }
            });
        });
    }

    function loadBatchesDropdown() {
        $.ajax({
            url: '/api/lookup/batches',
            method: 'GET',
            success: function (data) {
                sectionBatchCombobox.setItems(data.map(function (b) {
                    return { value: b.code, label: b.code, hint: b.name };
                }));
            }
        });
    }

    function loadCoursesDropdown() {
        $.ajax({
            url: '/api/lookup/courses',
            method: 'GET',
            success: function (data) {
                sectionCourseCombobox.setItems(data.map(function (c) {
                    return { value: c.name, label: c.name, hint: c.code };
                }));
            }
        });
    }

    /** Debounced "code will be ..." hint under the Course field. */
    function scheduleCoursePreview() {
        window.clearTimeout(coursePreviewTimer);
        const name = $('#sectionCourseInput').val().trim();
        if (!name) {
            $('#sectionCourseHelp').text(DEFAULT_COURSE_HELP);
            return;
        }
        coursePreviewTimer = window.setTimeout(function () {
            $.ajax({
                url: '/api/registrar/courses/preview-code?name=' + encodeURIComponent(name),
                method: 'GET',
                success: function (preview) {
                    if ($('#sectionCourseInput').val().trim() !== name) return; // stale response
                    $('#sectionCourseHelp').text(preview.existing
                        ? 'Existing course — code ' + preview.code + '.'
                        : 'New course — it will be created with code ' + preview.code + '.');
                }
            });
        }, 300);
    }
```

- [ ] **Step 6: Smoke-check** — `grep -rn "sectionBatchSelect\|sectionCourseSelect\|batchLookupList" src/main/resources/static` → expect **no output**.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/registrar.html src/main/resources/static/js/registrar-students.js src/main/resources/static/sections.html src/main/resources/static/js/registrar-sections.js
git commit -m "feat: custom combobox for batch assignment and section batch/course"
```

---

### Task 7: Wire comboboxes — student edit form, Documents picker; number check on Student Numbers page

**Files:** `static/student-records.html`, `static/js/registrar-student-records-edit.js`, `static/documents.html`, `static/js/registrar-documents.js`, `static/student-numbers.html`, `static/js/registrar-student-numbers.js`

- [ ] **Step 1: `student-records.html`** — remove the `list="…"` attribute and the `<datalist>…</datalist>` block from `#editSex`, `#editCivilStatus`, `#editCourseCode`, `#editSectionCode`. The four inputs become:

```html
                                            <input type="text" class="form-control" id="editSex" autocomplete="off">
```
```html
                                            <input type="text" class="form-control" id="editCivilStatus" autocomplete="off">
```
```html
                                            <input type="text" class="form-control" id="editCourseCode"
                                                autocomplete="off" placeholder="Type or pick a course code">
```
```html
                                            <input type="text" class="form-control" id="editSectionCode"
                                                autocomplete="off" placeholder="Type or pick a section code">
```
Bump `css/dashboard.css?v=3` → `?v=4`; replace the page script tag with:

```html
    <script src="js/combobox.js?v=1"></script>
    <script src="js/registrar-student-records-edit.js?v=9"></script>
```

- [ ] **Step 2: `js/registrar-student-records-edit.js`** — replace `loadOptions` and `loadAllLookups` (under `// ----- Lookup loaders -----`, ~lines 139–166) with:

```js
    // ----- Lookup comboboxes (custom dropdowns replacing native <datalist> popups) -----

    const comboboxes = {};

    function setupComboboxes() {
        comboboxes.sex = SrmsCombobox.attach(document.getElementById('editSex'), {
            items: ['Female', 'Male'].map(function (v) { return { value: v }; })
        });
        comboboxes.civilStatus = SrmsCombobox.attach(document.getElementById('editCivilStatus'), {
            items: ['Single', 'Married', 'Widowed', 'Separated'].map(function (v) { return { value: v }; })
        });
        comboboxes.course = SrmsCombobox.attach(document.getElementById('editCourseCode'), {
            emptyText: 'No course matches. New courses are created from the Sections page.'
        });
        comboboxes.section = SrmsCombobox.attach(document.getElementById('editSectionCode'), {
            emptyText: 'No section matches.'
        });
    }

    async function loadOptions(url, combobox) {
        try {
            const response = await fetch(url, { credentials: 'same-origin' });
            if (!response.ok) return;
            const items = await response.json();
            combobox.setItems(items.map(function (item) {
                return { value: item.code, label: item.code, hint: item.name };
            }));
        } catch (error) {
            // List stays empty; user can still type free text.
        }
    }

    async function loadAllLookups() {
        if (!comboboxes.course) setupComboboxes();
        await Promise.all([
            loadOptions('/api/lookup/courses', comboboxes.course),
            loadOptions('/api/lookup/sections', comboboxes.section)
        ]);
    }
```

- [ ] **Step 3: `documents.html`** — replace the upload student input + datalist with:

```html
                        <input type="text" class="form-control" id="uploadStudentId"
                               placeholder="Type a student ID or name" autocomplete="off">
```
Bump `css/dashboard.css?v=3` → `?v=4`; replace the page script tag with:

```html
    <script src="js/combobox.js?v=1"></script>
    <script src="js/registrar-documents.js?v=5"></script>
```

- [ ] **Step 4: `js/registrar-documents.js`** — add `let uploadStudentCombobox = null;` next to `let flatStudents = [];` (~line 22), and replace `rebuildFlatStudentsAndDatalist` (~line 891) with the version below. The item `value` stays the full `studentOptionLabel`, so the existing `#uploadStudentId` `input` handler (exact-label match → `selectedUploadStudentId`) keeps working unchanged, and read-only (locked) mode suppresses the menu automatically.

```js
    function rebuildFlatStudentsAndDatalist() {
        flatStudents = [];
        folderHierarchy.forEach(function (batch) {
            batch.sections.forEach(function (section) { flatStudents = flatStudents.concat(section.students); });
            flatStudents = flatStudents.concat(batch.unassignedStudents);
        });
        if (!uploadStudentCombobox) {
            uploadStudentCombobox = SrmsCombobox.attach(document.getElementById('uploadStudentId'), {
                emptyText: 'No student matches.'
            });
        }
        uploadStudentCombobox.setItems(flatStudents.map(function (s) {
            return {
                value: studentOptionLabel(s),
                label: s.lastName + ', ' + s.firstName,
                hint: s.studentNumber ? s.studentNumber : 'Ref: ' + s.studentId
            };
        }));
    }
```

- [ ] **Step 5: `student-numbers.html` + `js/registrar-student-numbers.js`** — replace the page script tag with:

```html
    <script src="js/student-number-check.js?v=1"></script>
    <script src="js/registrar-student-numbers.js?v=2"></script>
```
In the JS add `let numberCheck = null;` next to `let assignNumberModal = null;`. In `setupAssignStudentNumber`, after `assignNumberModal = new bootstrap.Modal(modalEl);`:

```js
        numberCheck = SrmsStudentNumberCheck.attach({
            input: inputEl,
            saveBtn: saveBtn,
            getRecordId: function () { return assignTargetRecordId; }
        });
```
In `openAssignNumberModal`, before `assignNumberModal.show();`: `if (numberCheck) numberCheck.reset();`

- [ ] **Step 6: Smoke-check** — `grep -rn "<datalist\|list=\"" src/main/resources/static --include=*.html` → expect **no output**.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/student-records.html src/main/resources/static/js/registrar-student-records-edit.js src/main/resources/static/documents.html src/main/resources/static/js/registrar-documents.js src/main/resources/static/student-numbers.html src/main/resources/static/js/registrar-student-numbers.js
git commit -m "feat: replace remaining datalists with custom combobox; inline student number check"
```

---

### Task 8: Verify, document, review, finish

- [ ] **Step 1: Full suite** (`superpowers:verification-before-completion`)

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, 0 failures. Baseline was 511; expect **546** (+10 generator, +9 section-create service, +6 course controller, +4 service & +2 controller student-number, +4 H2). Record the exact number.

- [ ] **Step 2: Live check** — `./gradlew bootRun` (MySQL on `localhost:3306` must be up), log in as `registrar` / `password123`, drive with Playwright MCP (`mcp__playwright__browser_*`). Screenshot items 1, 2, 3:
  1. **registrar.html → Assign Batch:** focusing the field opens the styled list (not the browser popup); typing filters; ↓/Enter picks; Esc closes the list but not the modal; a new code saves, and **reopening the modal lists it** without a page reload.
  2. **sections.html → Create Section:** Batch + Course show the custom list; typing `Bread and Pastry Production` shows "New course — it will be created with code BPP."; picking an existing course shows "Existing course — code …"; Create succeeds; the new course appears in the eligible-student Course filter; Admin → Logs has `Created course: Bread and Pastry Production (BPP)` and, for a new batch, `Created batch: …`.
  3. **Assign Student Number** (registrar.html and student-numbers.html): typing a number another student holds shows the red "already assigned to …" message and disables Save; editing the text re-enables Save; after clearing the other student's number, the same number saves.
  4. **student-records.html edit form:** Sex, Civil Status, Course, Section use the custom list; the list does not open while the form is read-only.
  5. **documents.html → Upload:** student picker uses the custom list; picking a student enables Upload; the locked (explorer) entry point shows no list.
  6. DevTools console: **no errors** on all five pages.
  Clean up any test course/batch/section created during the check only if the user asks (system_logs rows are append-only and stay).

- [ ] **Step 3: Memory bank + conventions** (newest entries at the **top** of each log):
  - `memory-bank/activeContext.md`, `progress.md`, `changeLog.md`, `testing.md` — this feature, branch, commits, test count, live-check results.
  - `memory-bank/decisions.md` — course-code derivation rule + input-matching order; `<datalist>` replaced by `SrmsCombobox`; availability check is advisory, the PUT is authoritative.
  - `memory-bank/systemPatterns.md` → *Front-End Patterns*: add "Type-or-pick fields use `js/combobox.js` (`SrmsCombobox.attach`), never `<datalist>`."
  - `CLAUDE.md` → *Key Conventions*: add a **Courses** line (created only from Create Section by name; code derived by `CourseCodeGenerator`) and a **Dropdowns** line (the `SrmsCombobox` rule above).

```bash
git add memory-bank CLAUDE.md
git commit -m "docs: record course auto-create, combobox, and student number guard"
```

- [ ] **Step 4: Review** — dispatch ECC `everything-claude-code:java-reviewer` on the backend diff (`git diff main...HEAD -- src/main/java src/test`) and `everything-claude-code:code-reviewer` on `src/main/resources/static`. Handle findings with `superpowers:receiving-code-review`; re-run the affected test classes after each fix.

- [ ] **Step 5: Finish** — invoke `superpowers:finishing-a-development-branch` (merge/PR decision is the user's; never push to `main` directly).

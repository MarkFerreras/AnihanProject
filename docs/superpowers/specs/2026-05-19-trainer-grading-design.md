# Design Spec: Trainer Grade Input + GWA Calculation

**Date:** 2026-05-19
**Status:** Approved
**Branch:** `feature/trainer-grade-input` (to be created from `main`)

---

## 1. Overview

Allow trainers to input student grades (midterm, finals, re-exam, hours studied, remarks) from the existing class roster modal on `trainer-classes.html`. The system auto-computes:

- **Final Grade** per subject: `(midterm × 0.40) + (finals × 0.60)`
- **GWA** per student: `Σ(effective_grade × units) / Σ(units)` across all graded subjects

Grading scale: **1.0** (highest) to **3.5** (failing). A grade > 3.0 is considered failed.

Trainers can **lock** grades to prevent accidental edits, and **unlock** to re-edit.

---

## 2. Grading Model

| Field | DB Type | Input | Description |
|-------|---------|-------|-------------|
| `midterm_grade` | DECIMAL(5,2) NULL | Trainer | Score on 1.0–5.0 scale |
| `finals_grade` | DECIMAL(5,2) NULL | Trainer | Score on 1.0–5.0 scale |
| `final_grade` | DECIMAL(5,2) NULL | Auto-computed | `(midterm × 0.40) + (finals × 0.60)` |
| `re_exam_grade` | DECIMAL(5,2) NULL | Trainer (optional) | For failed students; replaces final_grade as effective grade |
| `hours_studied` | DECIMAL(3,2) NULL | Trainer | Training hours |
| `remarks` | VARCHAR(255) NULL | Trainer | Free-text notes |
| `class_id` | INT NULL FK | System | Links grade to specific class (section+subject+semester) |
| `locked` | TINYINT(1) DEFAULT 0 | System | Lock toggle |
| `locked_at` | DATETIME NULL | System | Timestamp of last lock |

### Re-exam Logic

If `re_exam_grade` is present AND `final_grade > 3.0` (failed), the **effective grade** used for GWA is `re_exam_grade` instead of `final_grade`. The original `final_grade` is preserved for the academic record.

### GWA Formula

```
effective_grade = (re_exam_grade != null && final_grade > 3.0) ? re_exam_grade : final_grade
GWA = Σ(effective_grade × subject_units) / Σ(subject_units)
```

GWA is computed on-read (not stored). It aggregates across ALL classes the student has grades for.

### Grade Scale Reference

| Grade | Interpretation |
|-------|---------------|
| 1.0 | Excellent |
| 1.25 – 1.5 | Very Good |
| 1.75 – 2.0 | Good |
| 2.25 – 2.5 | Satisfactory |
| 2.75 – 3.0 | Passed |
| 3.5 | Failed |

---

## 3. Schema Migration

File: `src/main/sql/migrations/2026-05-19-grades-restructure.sql`

```sql
-- Idempotent migration: add class-scoped grading support to grades table.

-- Add class_id FK
ALTER TABLE grades ADD COLUMN IF NOT EXISTS class_id INT NULL;
ALTER TABLE grades ADD CONSTRAINT fk_grades_class
    FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE SET NULL;

-- Add component grade columns
ALTER TABLE grades ADD COLUMN IF NOT EXISTS midterm_grade DECIMAL(5,2) NULL;
ALTER TABLE grades ADD COLUMN IF NOT EXISTS finals_grade DECIMAL(5,2) NULL;

-- Add lock fields
ALTER TABLE grades ADD COLUMN IF NOT EXISTS locked TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE grades ADD COLUMN IF NOT EXISTS locked_at DATETIME NULL;

-- Relax existing NOT NULL constraints for backward compatibility
ALTER TABLE grades MODIFY COLUMN final_grade DECIMAL(5,2) NULL DEFAULT NULL;
ALTER TABLE grades MODIFY COLUMN hours_studied DECIMAL(3,2) NULL;
ALTER TABLE grades MODIFY COLUMN remarks VARCHAR(255) NULL;

-- Unique constraint: one grade per student per class
ALTER TABLE grades ADD UNIQUE KEY uq_grade_student_class (class_id, student_id);
```

`schema.sql` will be updated to reflect the final table definition.

---

## 4. Backend Architecture

### Entity: `Grade.java` (updated)

New fields added:
- `SchoolClass schoolClass` — `@ManyToOne` with `@JoinColumn(name = "class_id")`
- `BigDecimal midtermGrade`
- `BigDecimal finalsGrade`
- `boolean locked`
- `LocalDateTime lockedAt`

Existing fields preserved: `gradeId`, `student`, `subject`, `finalGrade`, `reExamGrade`, `hoursStudied`, `remarks`.

### Repository: `GradeRepository.java` (new)

```java
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    List<Grade> findBySchoolClassClassId(Integer classId);
    Optional<Grade> findBySchoolClassClassIdAndStudentStudentId(Integer classId, String studentId);
    List<Grade> findByStudentStudentId(String studentId);
    boolean existsBySchoolClassClassIdAndLockedTrue(Integer classId);
}
```

### Service: `TrainerService` (extended)

New methods:
- `getGradesForClass(Integer classId)` — returns all enrolled students with their grade data + per-student GWA
- `saveGrades(Integer classId, List<SaveGradeRequest> grades)` — bulk upsert; rejects if locked
- `lockGrades(Integer classId)` — sets `locked=true`, `locked_at=now()` on all grades for the class
- `unlockGrades(Integer classId)` — sets `locked=false`, `locked_at=null`

Private helpers:
- `computeFinalGrade(BigDecimal midterm, BigDecimal finals)` — applies 40/60 weight
- `getEffectiveGrade(Grade grade)` — returns re_exam if failed, otherwise final_grade
- `computeGwa(String studentId)` — aggregates across all graded subjects

### Controller: `TrainerController` (extended)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/trainer/classes/{classId}/grades` | Fetch all students + grades + GWA for a class |
| PUT | `/api/trainer/classes/{classId}/grades` | Bulk save/update grades |
| POST | `/api/trainer/classes/{classId}/grades/lock` | Lock all grades for the class |
| POST | `/api/trainer/classes/{classId}/grades/unlock` | Unlock all grades for the class |

### DTOs (in `dto/trainer/`)

**`SaveGradeRequest.java`** — request body per student:
- `String studentId` (required)
- `BigDecimal midtermGrade` (nullable, 1.0–5.0)
- `BigDecimal finalsGrade` (nullable, 1.0–5.0)
- `BigDecimal reExamGrade` (nullable, 1.0–5.0)
- `BigDecimal hoursStudied` (nullable, 0–9.99)
- `String remarks` (nullable, max 255 chars)

**`StudentGradeRow.java`** — response per student:
- `String studentId`
- `String lastName`, `firstName`, `middleName`
- `BigDecimal midtermGrade`, `finalsGrade`, `finalGrade`
- `BigDecimal reExamGrade`, `hoursStudied`
- `String remarks`
- `BigDecimal gwa` (computed)
- `boolean locked`

**`GradeSummaryResponse.java`** — wrapper for the class:
- `Integer classId`
- `String subjectName`, `sectionName`, `semester`
- `boolean locked`
- `List<StudentGradeRow> students`

### System Logs

Every grade-related action writes to `system_logs`:
- `"Saved grades for class #N (subject: X, section: Y)"`
- `"Locked grades for class #N"`
- `"Unlocked grades for class #N"`

---

## 5. Frontend Design

### HTML: `trainer-classes.html`

The existing `#classRosterModal` is enhanced:

**Modal header:** Title stays "Enrolled Students — {subjectName}"

**Table columns expand to:**
| Student ID | Last Name | First Name | Midterm | Finals | Final Grade | Re-exam | Hours | Remarks | GWA |

- **Midterm, Finals, Re-exam, Hours:** `<input type="number">` with `step="0.25"` for grades, appropriate min/max
- **Remarks:** `<input type="text">` with maxlength 255
- **Final Grade:** Read-only, auto-computed on client-side as inputs change
- **GWA:** Read-only, server-computed, displayed on load

**Modal footer buttons:**
- **Save Grades** — bulk PUT to save all grades
- **Lock Grades / Unlock Grades** — toggle button, changes label based on state
- **Close** — existing dismiss button

**Visual states:**
- When locked: all inputs disabled, lock button shows "Unlock Grades", subtle grey overlay
- Failing grades (> 3.0): `final_grade` cell highlighted with `text-danger`
- Success/error toast or inline alert after save/lock actions

### JavaScript: `trainer-classes.js`

Modified `openRoster()` flow:
1. Fetch `GET /api/trainer/classes/{classId}/grades` instead of `/students`
2. Populate table with editable inputs (or disabled if locked)
3. Attach `input` event listeners on midterm/finals to auto-compute final grade client-side
4. Wire Save button to `PUT /api/trainer/classes/{classId}/grades`
5. Wire Lock/Unlock button to appropriate POST endpoint
6. On successful save, refresh GWA from server response

---

## 6. Security

- All grade endpoints require `ROLE_TRAINER` via SecurityConfig
- Trainer ownership validation: trainer must be assigned to the class (existing pattern in `TrainerService.getStudentsForClass`)
- Locked grades reject PUT at the service layer (server-side enforcement, not just UI)
- CSRF disabled for `/api/**` (existing convention)
- Grade values validated server-side with Bean Validation (min 1.0, max 5.0)

---

## 7. Testing

### Unit Tests: `TrainerGradeServiceTest`
- `saveGrades_success` — valid grades saved, final_grade computed correctly
- `saveGrades_locked_rejected` — throws when class is locked
- `saveGrades_notMyClass_rejected` — throws when trainer doesn't own class
- `saveGrades_invalidGrade_rejected` — out of range values
- `lockGrades_success` — all grades marked locked
- `unlockGrades_success` — all grades unmarked
- `computeGwa_multipleSubjects` — correct weighted average
- `computeGwa_withReExam` — re-exam replaces failed final grade
- `getGradesForClass_newClass` — returns enrolled students with empty grades

### WebMvc Tests: `TrainerGradeControllerWebMvcTest`
- GET grades — 200 with grades, 403 for non-trainer, 400 for invalid classId
- PUT grades — 200 on save, 409 when locked, 403 for non-trainer
- POST lock — 200, POST unlock — 200
- RBAC: admin/registrar get 403

---

## 8. Out of Scope

- Registrar/admin grade views (future feature)
- Grade history/audit trail beyond system_logs
- PDF/Excel export of grades
- Per-component weight configuration (hardcoded 40/60)
- Grade appeal workflow

package com.example.springboot.config;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final SectionRepository sectionRepository;
    private final StudentRecordRepository studentRecordRepository;

    public DataSeeder(CourseRepository courseRepository,
                      BatchRepository batchRepository,
                      SectionRepository sectionRepository,
                      StudentRecordRepository studentRecordRepository) {
        this.courseRepository = courseRepository;
        this.batchRepository = batchRepository;
        this.sectionRepository = sectionRepository;
        this.studentRecordRepository = studentRecordRepository;
    }

    @Override
    public void run(String... args) {
        seedCourses();
        seedBatches();
        seedSections();
        seedStudentRecords();
    }

    private void seedCourses() {
        if (!courseRepository.existsById("CARS")) {
            courseRepository.save(new Course("CARS", "Culinary Arts and Restaurant Services"));
        }
    }

    private void seedBatches() {
        if (!batchRepository.existsById("B2024A")) {
            Batch b = new Batch();
            b.setBatchCode("B2024A");
            b.setBatchYear((short) 2024);
            batchRepository.save(b);
        }
        if (!batchRepository.existsById("B2025A")) {
            Batch b = new Batch();
            b.setBatchCode("B2025A");
            b.setBatchYear((short) 2025);
            batchRepository.save(b);
        }
        if (!batchRepository.existsById("B2026A")) {
            Batch b = new Batch();
            b.setBatchCode("B2026A");
            b.setBatchYear((short) 2026);
            batchRepository.save(b);
        }
    }

    private void seedSections() {
        Course cars = courseRepository.findById("CARS").orElse(null);
        if (cars == null) return;

        if (!sectionRepository.existsById("SEC-A24")) {
            batchRepository.findById("B2024A").ifPresent(batch -> {
                Section s = new Section();
                s.setSectionCode("SEC-A24");
                s.setSection("Section A 2024");
                s.setBatch(batch);
                s.setCourse(cars);
                sectionRepository.save(s);
            });
        }
        if (!sectionRepository.existsById("SEC-A25")) {
            batchRepository.findById("B2025A").ifPresent(batch -> {
                Section s = new Section();
                s.setSectionCode("SEC-A25");
                s.setSection("Section A 2025");
                s.setBatch(batch);
                s.setCourse(cars);
                sectionRepository.save(s);
            });
        }
        if (!sectionRepository.existsById("SEC-A26")) {
            batchRepository.findById("B2026A").ifPresent(batch -> {
                Section s = new Section();
                s.setSectionCode("SEC-A26");
                s.setSection("Section A 2026");
                s.setBatch(batch);
                s.setCourse(cars);
                sectionRepository.save(s);
            });
        }
    }

    private void seedStudentRecords() {
        Course cars = courseRepository.findById("CARS").orElse(null);
        if (cars == null) return;

        Batch b2024 = batchRepository.findById("B2024A").orElse(null);
        Batch b2025 = batchRepository.findById("B2025A").orElse(null);
        Batch b2026 = batchRepository.findById("B2026A").orElse(null);
        Section sec24 = sectionRepository.findById("SEC-A24").orElse(null);
        Section sec25 = sectionRepository.findById("SEC-A25").orElse(null);
        Section sec26 = sectionRepository.findById("SEC-A26").orElse(null);

        if (studentRecordRepository.findByStudentId("STU-2024-001").isEmpty()) {
            StudentRecord r = new StudentRecord();
            r.setStudentId("STU-2024-001");
            r.setLastName("Reyes");
            r.setFirstName("Anna");
            r.setMiddleName("Cruz");
            r.setBirthdate(LocalDate.of(2003, 4, 12));
            r.setAge(22);
            r.setSex("Female");
            r.setCivilStatus("Single");
            r.setPermanentAddress("123 Mabini St, Quezon City");
            r.setTemporaryAddress("45 Aurora Blvd, Manila");
            r.setEmail("anna.reyes@example.com");
            r.setContactNo("09171234001");
            r.setReligion("Roman Catholic");
            r.setBaptized(true);
            r.setBaptismDate(LocalDate.of(2003, 6, 20));
            r.setBaptismPlace("San Pedro Parish, Manila");
            r.setSiblingCount(2);
            r.setBrotherCount(1);
            r.setSisterCount(1);
            r.setBatch(b2024);
            r.setCourse(cars);
            r.setSection(sec24);
            r.setProfilePicture(new byte[0]);
            r.setEnrollmentDate(LocalDate.of(2024, 6, 3));
            r.setStudentStatus("Active");
            studentRecordRepository.save(r);
        }

        if (studentRecordRepository.findByStudentId("STU-2024-002").isEmpty()) {
            StudentRecord r = new StudentRecord();
            r.setStudentId("STU-2024-002");
            r.setLastName("Santos");
            r.setFirstName("Bea");
            r.setMiddleName("Lim");
            r.setBirthdate(LocalDate.of(2002, 9, 30));
            r.setAge(23);
            r.setSex("Female");
            r.setCivilStatus("Single");
            r.setPermanentAddress("88 Roxas Ave, Pasig");
            r.setTemporaryAddress("12 EDSA, Mandaluyong");
            r.setEmail("bea.santos@example.com");
            r.setContactNo("09171234002");
            r.setReligion("Iglesia ni Cristo");
            r.setBaptized(true);
            r.setBaptismDate(LocalDate.of(2003, 1, 15));
            r.setBaptismPlace("INC Central Temple, Quezon City");
            r.setSiblingCount(3);
            r.setBrotherCount(2);
            r.setSisterCount(1);
            r.setBatch(b2024);
            r.setCourse(cars);
            r.setSection(sec24);
            r.setProfilePicture(new byte[0]);
            r.setEnrollmentDate(LocalDate.of(2024, 6, 3));
            r.setStudentStatus("Active");
            studentRecordRepository.save(r);
        }

        if (studentRecordRepository.findByStudentId("STU-2025-001").isEmpty()) {
            StudentRecord r = new StudentRecord();
            r.setStudentId("STU-2025-001");
            r.setLastName("Cruz");
            r.setFirstName("Carla");
            r.setMiddleName("Mendoza");
            r.setBirthdate(LocalDate.of(2004, 1, 18));
            r.setAge(22);
            r.setSex("Female");
            r.setCivilStatus("Single");
            r.setPermanentAddress("7 Bonifacio St, Makati");
            r.setTemporaryAddress("7 Bonifacio St, Makati");
            r.setEmail("carla.cruz@example.com");
            r.setContactNo("09171234003");
            r.setReligion("Christian");
            r.setBaptized(true);
            r.setBaptismDate(LocalDate.of(2004, 5, 10));
            r.setBaptismPlace("Christ Fellowship Church, Makati");
            r.setSiblingCount(1);
            r.setBrotherCount(0);
            r.setSisterCount(1);
            r.setBatch(b2025);
            r.setCourse(cars);
            r.setSection(sec25);
            r.setProfilePicture(new byte[0]);
            r.setEnrollmentDate(LocalDate.of(2025, 6, 2));
            r.setStudentStatus("Active");
            studentRecordRepository.save(r);
        }

        if (studentRecordRepository.findByStudentId("STU-2025-002").isEmpty()) {
            StudentRecord r = new StudentRecord();
            r.setStudentId("STU-2025-002");
            r.setLastName("Garcia");
            r.setFirstName("Diana");
            r.setMiddleName("Reyes");
            r.setBirthdate(LocalDate.of(2003, 12, 5));
            r.setAge(22);
            r.setSex("Female");
            r.setCivilStatus("Single");
            r.setPermanentAddress("256 Espana Blvd, Manila");
            r.setTemporaryAddress("256 Espana Blvd, Manila");
            r.setEmail("diana.garcia@example.com");
            r.setContactNo("09171234004");
            r.setReligion("Roman Catholic");
            r.setBaptized(true);
            r.setBaptismDate(LocalDate.of(2004, 2, 28));
            r.setBaptismPlace("Sto. Domingo Church, Manila");
            r.setSiblingCount(4);
            r.setBrotherCount(2);
            r.setSisterCount(2);
            r.setBatch(b2025);
            r.setCourse(cars);
            r.setSection(sec25);
            r.setProfilePicture(new byte[0]);
            r.setEnrollmentDate(LocalDate.of(2025, 6, 2));
            r.setStudentStatus("Active");
            studentRecordRepository.save(r);
        }

        if (studentRecordRepository.findByStudentId("STU-2026-001").isEmpty()) {
            StudentRecord r = new StudentRecord();
            r.setStudentId("STU-2026-001");
            r.setLastName("Lopez");
            r.setFirstName("Elise");
            r.setMiddleName("Tan");
            r.setBirthdate(LocalDate.of(2005, 7, 22));
            r.setAge(20);
            r.setSex("Female");
            r.setCivilStatus("Single");
            r.setPermanentAddress("19 Katipunan Ave, Quezon City");
            r.setTemporaryAddress("19 Katipunan Ave, Quezon City");
            r.setEmail("elise.lopez@example.com");
            r.setContactNo("09171234005");
            r.setReligion("Roman Catholic");
            r.setBaptized(true);
            r.setBaptismDate(LocalDate.of(2005, 10, 14));
            r.setBaptismPlace("Mary Immaculate Parish, Quezon City");
            r.setSiblingCount(2);
            r.setBrotherCount(0);
            r.setSisterCount(2);
            r.setBatch(b2026);
            r.setCourse(cars);
            r.setSection(sec26);
            r.setProfilePicture(new byte[0]);
            r.setEnrollmentDate(LocalDate.of(2026, 6, 1));
            r.setStudentStatus("Active");
            studentRecordRepository.save(r);
        }
    }
}

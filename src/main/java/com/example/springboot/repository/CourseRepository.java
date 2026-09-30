package com.example.springboot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Course;

public interface CourseRepository extends JpaRepository<Course, String> {

    /** course_name is not unique in the schema, so take the first match. */
    Optional<Course> findFirstByCourseNameIgnoreCase(String courseName);
}

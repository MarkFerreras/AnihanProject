package com.example.springboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot.model.Grade;

public interface GradeRepository extends JpaRepository<Grade, Integer> {
    List<Grade> findBySchoolClassClassId(Integer classId);

    Optional<Grade> findBySchoolClassClassIdAndStudentStudentId(Integer classId, String studentId);

    List<Grade> findByStudentStudentId(String studentId);

    /**
     * How many distinct classes taught by this trainer still hold at least one
     * locked grade. Used by {@code AdminService} to block a trainer hard-delete
     * that would orphan grades no one could ever unlock again.
     */
    @Query(value = "SELECT COUNT(DISTINCT g.class_id) FROM grades g "
            + "JOIN classes c ON g.class_id = c.class_id "
            + "WHERE c.trainer_id = :trainerId AND g.locked = 1", nativeQuery = true)
    long countLockedGradeClassesByTrainerId(@Param("trainerId") Integer trainerId);
}

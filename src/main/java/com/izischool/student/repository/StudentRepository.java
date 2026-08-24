package com.izischool.student.repository;

import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndSchool_IdAndDeletedFalse(UUID id, UUID schoolId);

    Optional<Student> findBySchool_IdAndStudentNumberAndDeletedFalse(UUID schoolId, String studentNumber);

    Page<Student> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    Page<Student> findBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, StudentStatus status, Pageable pageable);

    boolean existsBySchool_IdAndStudentNumber(UUID schoolId, String studentNumber);

    long countBySchool_IdAndDeletedFalse(UUID schoolId);

    long countBySchool_Id(UUID schoolId);

    @Query("SELECT s FROM Student s WHERE s.school.id = :schoolId AND s.deleted = false AND (" +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Student> searchStudents(@Param("schoolId") UUID schoolId, @Param("query") String query, Pageable pageable);
}

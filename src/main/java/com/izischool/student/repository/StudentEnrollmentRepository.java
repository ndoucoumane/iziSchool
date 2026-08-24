package com.izischool.student.repository;

import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.StudentEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    Optional<StudentEnrollment> findByIdAndSchool_Id(UUID id, UUID schoolId);

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    Optional<StudentEnrollment> findBySchool_IdAndStudent_IdAndAcademicYear_Id(UUID schoolId, UUID studentId, UUID academicYearId);

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    List<StudentEnrollment> findBySchool_IdAndAcademicYear_IdAndSchoolClass_Id(UUID schoolId, UUID academicYearId, UUID schoolClassId);

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    Page<StudentEnrollment> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    Page<StudentEnrollment> findBySchool_IdAndAcademicYear_IdAndStatus(UUID schoolId, UUID academicYearId, EnrollmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    Page<StudentEnrollment> findBySchool_IdAndAcademicYear_IdAndSchoolClass_Id(UUID schoolId, UUID academicYearId, UUID schoolClassId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    List<StudentEnrollment> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId);

    @EntityGraph(attributePaths = {"student", "academicYear", "schoolClass"})
    List<StudentEnrollment> findBySchool_Id(UUID schoolId);

    boolean existsBySchool_IdAndStudent_IdAndAcademicYear_Id(UUID schoolId, UUID studentId, UUID academicYearId);

    long countBySchool_IdAndAcademicYear_IdAndStatus(UUID schoolId, UUID academicYearId, EnrollmentStatus status);

    long countBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId);

    long countBySchool_IdAndAcademicYear_IdAndSchoolClass_Id(UUID schoolId, UUID academicYearId, UUID schoolClassId);

    long countBySchool_IdAndAcademicYear_IdAndSchoolClass_IdAndStatus(UUID schoolId, UUID academicYearId, UUID schoolClassId, EnrollmentStatus status);
}

package com.izischool.student.repository;

import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.StudentEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    Optional<StudentEnrollment> findByIdAndSchool_Id(UUID id, UUID schoolId);

    Optional<StudentEnrollment> findBySchool_IdAndStudent_IdAndAcademicYear_Id(UUID schoolId, UUID studentId, UUID academicYearId);

    List<StudentEnrollment> findBySchool_IdAndAcademicYear_IdAndSchoolClass_Id(UUID schoolId, UUID academicYearId, UUID schoolClassId);

    Page<StudentEnrollment> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId, Pageable pageable);

    Page<StudentEnrollment> findBySchool_IdAndAcademicYear_IdAndStatus(UUID schoolId, UUID academicYearId, EnrollmentStatus status, Pageable pageable);

    boolean existsBySchool_IdAndStudent_IdAndAcademicYear_Id(UUID schoolId, UUID studentId, UUID academicYearId);

    long countBySchool_IdAndAcademicYear_IdAndStatus(UUID schoolId, UUID academicYearId, EnrollmentStatus status);
}

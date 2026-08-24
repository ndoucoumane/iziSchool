package com.izischool.academic.repository;

import com.izischool.academic.domain.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    @EntityGraph(attributePaths = {"academicYear", "gradeLevel"})
    Optional<SchoolClass> findByIdAndSchool_Id(UUID id, UUID schoolId);

    @EntityGraph(attributePaths = {"academicYear", "gradeLevel"})
    List<SchoolClass> findBySchool_Id(UUID schoolId);

    @EntityGraph(attributePaths = {"academicYear", "gradeLevel"})
    List<SchoolClass> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId);

    @EntityGraph(attributePaths = {"academicYear", "gradeLevel"})
    List<SchoolClass> findBySchool_IdAndAcademicYear_IdAndGradeLevel_Id(UUID schoolId, UUID academicYearId, UUID gradeLevelId);

    @EntityGraph(attributePaths = {"academicYear", "gradeLevel"})
    Page<SchoolClass> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId, Pageable pageable);

    boolean existsBySchool_IdAndAcademicYear_IdAndCode(UUID schoolId, UUID academicYearId, String code);
}

package com.izischool.academic.repository;

import com.izischool.academic.domain.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    Optional<SchoolClass> findByIdAndSchool_Id(UUID id, UUID schoolId);

    List<SchoolClass> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId);

    List<SchoolClass> findBySchool_IdAndAcademicYear_IdAndGradeLevel_Id(UUID schoolId, UUID academicYearId, UUID gradeLevelId);

    Page<SchoolClass> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId, Pageable pageable);

    boolean existsBySchool_IdAndAcademicYear_IdAndCode(UUID schoolId, UUID academicYearId, String code);
}

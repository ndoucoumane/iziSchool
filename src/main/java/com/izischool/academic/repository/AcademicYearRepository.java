package com.izischool.academic.repository;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    Optional<AcademicYear> findByIdAndSchool_Id(UUID id, UUID schoolId);

    Optional<AcademicYear> findBySchool_IdAndStatus(UUID schoolId, AcademicYearStatus status);

    List<AcademicYear> findBySchool_IdOrderByStartDateDesc(UUID schoolId);

    Page<AcademicYear> findBySchool_Id(UUID schoolId, Pageable pageable);

    boolean existsBySchool_IdAndName(UUID schoolId, String name);

    boolean existsBySchool_IdAndStatus(UUID schoolId, AcademicYearStatus status);
}

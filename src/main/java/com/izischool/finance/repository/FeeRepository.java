package com.izischool.finance.repository;

import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeRepository extends JpaRepository<Fee, UUID> {

    @EntityGraph(attributePaths = {"academicYear"})
    Optional<Fee> findByIdAndSchool_Id(UUID id, UUID schoolId);

    @EntityGraph(attributePaths = {"academicYear"})
    List<Fee> findBySchool_Id(UUID schoolId);

    @EntityGraph(attributePaths = {"academicYear"})
    List<Fee> findBySchool_IdAndActiveTrue(UUID schoolId);

    @EntityGraph(attributePaths = {"academicYear"})
    List<Fee> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId);

    @EntityGraph(attributePaths = {"academicYear"})
    List<Fee> findBySchool_IdAndAcademicYear_IdAndActiveTrue(UUID schoolId, UUID academicYearId);

    @EntityGraph(attributePaths = {"academicYear"})
    List<Fee> findBySchool_IdAndAcademicYear_IdAndFeeTypeAndActiveTrue(UUID schoolId, UUID academicYearId, FeeType feeType);

    @EntityGraph(attributePaths = {"academicYear"})
    Page<Fee> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId, Pageable pageable);

    boolean existsBySchool_IdAndAcademicYear_IdAndCode(UUID schoolId, UUID academicYearId, String code);
}

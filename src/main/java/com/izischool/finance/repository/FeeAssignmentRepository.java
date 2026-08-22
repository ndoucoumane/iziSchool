package com.izischool.finance.repository;

import com.izischool.finance.domain.FeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeAssignmentRepository extends JpaRepository<FeeAssignment, UUID> {

    Optional<FeeAssignment> findByIdAndSchool_Id(UUID id, UUID schoolId);

    List<FeeAssignment> findBySchool_IdAndAcademicYear_IdAndActiveTrue(UUID schoolId, UUID academicYearId);

    @Query("SELECT fa FROM FeeAssignment fa WHERE fa.school.id = :schoolId AND fa.fee.id = :feeId " +
           "AND fa.schoolClass.id = :classId AND fa.active = true")
    Optional<FeeAssignment> findByClassOverride(@Param("schoolId") UUID schoolId, @Param("feeId") UUID feeId, @Param("classId") UUID classId);

    @Query("SELECT fa FROM FeeAssignment fa WHERE fa.school.id = :schoolId AND fa.fee.id = :feeId " +
           "AND fa.gradeLevel.id = :gradeLevelId AND fa.schoolClass IS NULL AND fa.active = true")
    Optional<FeeAssignment> findByGradeLevelAssignment(@Param("schoolId") UUID schoolId, @Param("feeId") UUID feeId, @Param("gradeLevelId") UUID gradeLevelId);
}

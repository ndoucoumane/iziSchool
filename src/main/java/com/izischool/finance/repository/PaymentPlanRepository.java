package com.izischool.finance.repository;

import com.izischool.finance.domain.PaymentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, UUID> {

    Optional<PaymentPlan> findByIdAndSchool_Id(UUID id, UUID schoolId);

    List<PaymentPlan> findBySchool_IdAndAcademicYear_IdAndActiveTrue(UUID schoolId, UUID academicYearId);

    Page<PaymentPlan> findBySchool_IdAndAcademicYear_Id(UUID schoolId, UUID academicYearId, Pageable pageable);

    boolean existsBySchool_IdAndAcademicYear_IdAndName(UUID schoolId, UUID academicYearId, String name);
}

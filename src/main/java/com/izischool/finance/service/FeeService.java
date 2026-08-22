package com.izischool.finance.service;

import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.util.MoneyUtils;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeAssignment;
import com.izischool.finance.repository.FeeAssignmentRepository;
import com.izischool.finance.repository.FeeRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRepository feeRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public Fee createFee(Fee fee) {
        tenantValidationService.validateEntityAccess(fee);
        UUID schoolId = fee.getSchool().getId();
        UUID yearId = fee.getAcademicYear().getId();

        if (feeRepository.existsBySchool_IdAndAcademicYear_IdAndCode(schoolId, yearId, fee.getCode())) {
            throw new ConflictException(String.format("Fee with code [%s] already exists for this academic year", fee.getCode()));
        }

        fee.setAmount(MoneyUtils.scale(fee.getAmount()));
        return feeRepository.save(fee);
    }

    @Transactional
    public FeeAssignment createFeeAssignment(FeeAssignment assignment) {
        tenantValidationService.validateEntityAccess(assignment);
        assignment.setAmount(MoneyUtils.scale(assignment.getAmount()));
        return feeAssignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public Fee getFeeById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return feeRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee", id));
    }

    @Transactional(readOnly = true)
    public List<Fee> getActiveFeesByAcademicYear(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return feeRepository.findBySchool_IdAndAcademicYear_IdAndActiveTrue(schoolId, academicYearId);
    }

    /**
     * Resolves the effective amount for a fee given a class and grade level.
     * Priority: SchoolClass assignment > GradeLevel assignment > Fee base amount.
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveEffectiveFeeAmount(UUID schoolId, UUID feeId, UUID gradeLevelId, UUID classId) {
        tenantValidationService.validateSchoolAccess(schoolId);

        // 1. Check class override
        if (classId != null) {
            Optional<FeeAssignment> classAssignment = feeAssignmentRepository.findByClassOverride(schoolId, feeId, classId);
            if (classAssignment.isPresent()) {
                return classAssignment.get().getAmount();
            }
        }

        // 2. Check grade level assignment
        if (gradeLevelId != null) {
            Optional<FeeAssignment> gradeAssignment = feeAssignmentRepository.findByGradeLevelAssignment(schoolId, feeId, gradeLevelId);
            if (gradeAssignment.isPresent()) {
                return gradeAssignment.get().getAmount();
            }
        }

        // 3. Fallback to base fee amount
        Fee baseFee = getFeeById(feeId, schoolId);
        return baseFee.getAmount();
    }
}

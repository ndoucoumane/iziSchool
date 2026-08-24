package com.izischool.academic.service;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.academic.repository.AcademicYearRepository;
import com.izischool.common.exception.BusinessException;
import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public AcademicYear createAcademicYear(AcademicYear academicYear) {
        tenantValidationService.validateEntityAccess(academicYear);

        if (academicYear.getStartDate().isAfter(academicYear.getEndDate())) {
            throw new BusinessException("Start date must be before end date");
        }

        UUID schoolId = academicYear.getSchool().getId();
        if (academicYearRepository.existsBySchool_IdAndName(schoolId, academicYear.getName())) {
            throw new ConflictException(String.format("Academic year [%s] already exists for this school", academicYear.getName()));
        }

        if (academicYear.getStatus() == AcademicYearStatus.ACTIVE) {
            Optional<AcademicYear> currentActive = academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE);
            if (currentActive.isPresent()) {
                throw new BusinessException("An active academic year already exists. Please close the active year first.");
            }
        }

        return academicYearRepository.save(academicYear);
    }

    @Transactional
    public AcademicYear updateAcademicYear(UUID id, UUID schoolId, AcademicYear updatedData) {
        tenantValidationService.validateSchoolAccess(schoolId);
        AcademicYear year = getAcademicYearById(id, schoolId);

        if (updatedData.getStartDate() != null && updatedData.getEndDate() != null) {
            if (updatedData.getStartDate().isAfter(updatedData.getEndDate())) {
                throw new BusinessException("Start date must be before end date");
            }
            year.setStartDate(updatedData.getStartDate());
            year.setEndDate(updatedData.getEndDate());
        }

        if (updatedData.getName() != null && !updatedData.getName().equals(year.getName())) {
            if (academicYearRepository.existsBySchool_IdAndName(schoolId, updatedData.getName())) {
                throw new ConflictException(String.format("Academic year [%s] already exists for this school", updatedData.getName()));
            }
            year.setName(updatedData.getName());
        }

        return academicYearRepository.save(year);
    }

    @Transactional
    public AcademicYear activateAcademicYear(UUID academicYearId, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        AcademicYear year = getAcademicYearById(academicYearId, schoolId);

        // Deactivate currently active year if any
        academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE)
                .ifPresent(activeYear -> {
                    activeYear.setStatus(AcademicYearStatus.CLOSED);
                    academicYearRepository.save(activeYear);
                    log.info("Closed previous academic year {} for school {}", activeYear.getName(), schoolId);
                });

        year.setStatus(AcademicYearStatus.ACTIVE);
        return academicYearRepository.save(year);
    }

    @Transactional
    public AcademicYear closeAcademicYear(UUID academicYearId, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        AcademicYear year = getAcademicYearById(academicYearId, schoolId);
        year.setStatus(AcademicYearStatus.CLOSED);
        log.info("Closed academic year {} for school {}", year.getName(), schoolId);
        return academicYearRepository.save(year);
    }

    @Transactional(readOnly = true)
    public AcademicYear getAcademicYearById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return academicYearRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", id));
    }

    @Transactional(readOnly = true)
    public Optional<AcademicYear> getActiveAcademicYear(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<AcademicYear> getAcademicYearsBySchool(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return academicYearRepository.findBySchool_IdOrderByStartDateDesc(schoolId);
    }
}

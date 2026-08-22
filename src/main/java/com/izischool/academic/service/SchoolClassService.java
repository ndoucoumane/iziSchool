package com.izischool.academic.service;

import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import com.izischool.academic.repository.SchoolClassRepository;
import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public SchoolClass createSchoolClass(SchoolClass schoolClass) {
        tenantValidationService.validateEntityAccess(schoolClass);
        UUID schoolId = schoolClass.getSchool().getId();
        UUID yearId = schoolClass.getAcademicYear().getId();

        if (schoolClassRepository.existsBySchool_IdAndAcademicYear_IdAndCode(schoolId, yearId, schoolClass.getCode())) {
            throw new ConflictException(String.format("Class with code [%s] already exists for this academic year", schoolClass.getCode()));
        }

        if (schoolClass.getStatus() == null) {
            schoolClass.setStatus(SchoolClassStatus.ACTIVE);
        }

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional(readOnly = true)
    public SchoolClass getSchoolClassById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return schoolClassRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass", id));
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> getClassesByAcademicYear(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return schoolClassRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYearId);
    }
}

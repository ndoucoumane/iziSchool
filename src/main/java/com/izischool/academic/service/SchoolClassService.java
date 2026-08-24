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
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<SchoolClass> getClassesFiltered(UUID schoolId, UUID academicYearId, String name, String level, SchoolClassStatus status) {
        tenantValidationService.validateSchoolAccess(schoolId);
        List<SchoolClass> list = (academicYearId != null)
                ? schoolClassRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYearId)
                : schoolClassRepository.findBySchool_Id(schoolId);

        return list.stream()
                .filter(c -> name == null || c.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(c -> level == null || (c.getGradeLevel() != null && (c.getGradeLevel().getName().equalsIgnoreCase(level) || c.getGradeLevel().getCode().equalsIgnoreCase(level))))
                .filter(c -> status == null || c.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Transactional
    public SchoolClass updateSchoolClass(UUID id, UUID schoolId, SchoolClass updatedData) {
        tenantValidationService.validateSchoolAccess(schoolId);
        SchoolClass schoolClass = getSchoolClassById(id, schoolId);

        if (updatedData.getName() != null) schoolClass.setName(updatedData.getName());
        if (updatedData.getCapacity() > 0) schoolClass.setCapacity(updatedData.getCapacity());
        if (updatedData.getStatus() != null) schoolClass.setStatus(updatedData.getStatus());
        if (updatedData.getGradeLevel() != null) schoolClass.setGradeLevel(updatedData.getGradeLevel());

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public void deactivateSchoolClass(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        SchoolClass schoolClass = getSchoolClassById(id, schoolId);
        schoolClass.setStatus(SchoolClassStatus.INACTIVE);
        schoolClassRepository.save(schoolClass);
        log.info("Archived school class {} for school {}", schoolClass.getName(), schoolId);
    }
}

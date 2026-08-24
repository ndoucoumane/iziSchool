package com.izischool.academic.service;

import com.izischool.academic.domain.GradeLevel;
import com.izischool.academic.repository.GradeLevelRepository;
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
public class GradeLevelService {

    private final GradeLevelRepository gradeLevelRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public GradeLevel createGradeLevel(GradeLevel gradeLevel) {
        tenantValidationService.validateEntityAccess(gradeLevel);
        UUID schoolId = gradeLevel.getSchool().getId();

        if (gradeLevelRepository.existsBySchool_IdAndCode(schoolId, gradeLevel.getCode())) {
            throw new ConflictException(String.format("Grade level with code [%s] already exists in this school", gradeLevel.getCode()));
        }

        return gradeLevelRepository.save(gradeLevel);
    }

    @Transactional(readOnly = true)
    public GradeLevel getGradeLevelById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return gradeLevelRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("GradeLevel", id));
    }

    @Transactional(readOnly = true)
    public List<GradeLevel> getActiveGradeLevels(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return gradeLevelRepository.findBySchool_IdAndActiveTrueOrderByDisplayOrderAsc(schoolId);
    }
}

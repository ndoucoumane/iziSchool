package com.izischool.school.service;

import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    @Transactional
    public School createSchool(School school) {
        if (schoolRepository.existsByCode(school.getCode())) {
            throw new ConflictException(String.format("School with code [%s] already exists", school.getCode()));
        }
        if (school.getStatus() == null) {
            school.setStatus(SchoolStatus.ACTIVE);
        }
        if (school.getCurrency() == null || school.getCurrency().isBlank()) {
            school.setCurrency("XOF");
        }
        School saved = schoolRepository.save(school);
        log.info("Created new school tenant: {} ({})", saved.getName(), saved.getCode());
        return saved;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "school_info", key = "#id")
    public School getSchoolById(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School", id));
    }

    @Transactional(readOnly = true)
    public School getSchoolByCode(String code) {
        return schoolRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("School with code [%s] not found", code)));
    }

    @Transactional(readOnly = true)
    public Page<School> getAllSchools(Pageable pageable) {
        return schoolRepository.findAll(pageable);
    }

    @Transactional
    @CacheEvict(value = "school_info", key = "#id")
    public School updateSchoolStatus(UUID id, SchoolStatus status) {
        School school = getSchoolById(id);
        school.setStatus(status);
        log.info("Updated status for school {} to {}", school.getCode(), status);
        return schoolRepository.save(school);
    }
}

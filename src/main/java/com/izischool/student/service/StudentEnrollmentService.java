package com.izischool.student.service;

import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.student.repository.StudentEnrollmentRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentEnrollmentService {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public StudentEnrollment enrollStudent(StudentEnrollment enrollment) {
        tenantValidationService.validateEntityAccess(enrollment);
        UUID schoolId = enrollment.getSchool().getId();
        UUID studentId = enrollment.getStudent().getId();
        UUID academicYearId = enrollment.getAcademicYear().getId();

        if (enrollmentRepository.existsBySchool_IdAndStudent_IdAndAcademicYear_Id(schoolId, studentId, academicYearId)) {
            throw new ConflictException("Student is already enrolled in an academic class for this year");
        }

        if (enrollment.getEnrollmentDate() == null) {
            enrollment.setEnrollmentDate(LocalDate.now());
        }
        if (enrollment.getStatus() == null) {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
        }

        StudentEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrolled student {} into class {} for academic year {}",
                saved.getStudent().getStudentNumber(),
                saved.getSchoolClass().getName(),
                saved.getAcademicYear().getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public StudentEnrollment getEnrollmentById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return enrollmentRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentEnrollment", id));
    }

    @Transactional(readOnly = true)
    public List<StudentEnrollment> getEnrollmentsByClass(UUID schoolId, UUID academicYearId, UUID classId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return enrollmentRepository.findBySchool_IdAndAcademicYear_IdAndSchoolClass_Id(schoolId, academicYearId, classId);
    }

    @Transactional(readOnly = true)
    public Page<StudentEnrollment> getEnrollmentsByAcademicYear(UUID schoolId, UUID academicYearId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return enrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYearId, pageable);
    }
}

package com.izischool.student.service;

import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.util.ReferenceGenerator;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.repository.StudentRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public Student createStudent(Student student) {
        tenantValidationService.validateEntityAccess(student);
        UUID schoolId = student.getSchool().getId();

        if (student.getStudentNumber() == null || student.getStudentNumber().isBlank()) {
            long count = studentRepository.countBySchool_IdAndDeletedFalse(schoolId);
            student.setStudentNumber(ReferenceGenerator.generateStudentNumber(student.getSchool().getCode(), count + 1));
        } else if (studentRepository.existsBySchool_IdAndStudentNumber(schoolId, student.getStudentNumber())) {
            throw new ConflictException(String.format("Student with number [%s] already exists in this school", student.getStudentNumber()));
        }

        if (student.getStatus() == null) {
            student.setStatus(StudentStatus.ACTIVE);
        }

        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public Student getStudentById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return studentRepository.findByIdAndSchool_IdAndDeletedFalse(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    @Transactional(readOnly = true)
    public Student getStudentByNumber(String studentNumber, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return studentRepository.findBySchool_IdAndStudentNumberAndDeletedFalse(schoolId, studentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Student with number [%s] not found", studentNumber)));
    }

    @Transactional(readOnly = true)
    public Page<Student> getStudentsBySchool(UUID schoolId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return studentRepository.findBySchool_IdAndDeletedFalse(schoolId, pageable);
    }

    @Transactional
    public void deleteStudent(UUID id, UUID schoolId) {
        Student student = getStudentById(id, schoolId);
        student.softDelete();
        studentRepository.save(student);
        log.info("Soft-deleted student {} from school {}", student.getStudentNumber(), schoolId);
    }
}

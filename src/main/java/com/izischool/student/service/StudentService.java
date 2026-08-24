package com.izischool.student.service;

import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.util.ReferenceGenerator;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.dto.StudentBalanceResponse;
import com.izischool.student.repository.StudentRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public Student createStudent(Student student) {
        tenantValidationService.validateEntityAccess(student);
        UUID schoolId = student.getSchool().getId();

        if (student.getStudentNumber() == null || student.getStudentNumber().isBlank()) {
            long sequence = studentRepository.countBySchool_Id(schoolId) + 1;
            String schoolCode = student.getSchool().getCode();
            String candidateNumber;
            do {
                candidateNumber = ReferenceGenerator.generateStudentNumber(schoolCode, sequence++);
            } while (studentRepository.existsBySchool_IdAndStudentNumber(schoolId, candidateNumber));

            student.setStudentNumber(candidateNumber);
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

    @Transactional(readOnly = true)
    public Page<Student> getStudentsPaged(UUID schoolId, String search, StudentStatus status, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        if (search != null && !search.isBlank()) {
            return studentRepository.searchStudents(schoolId, search, pageable);
        }
        if (status != null) {
            return studentRepository.findBySchool_IdAndStatusAndDeletedFalse(schoolId, status, pageable);
        }
        return studentRepository.findBySchool_IdAndDeletedFalse(schoolId, pageable);
    }

    @Transactional
    public Student updateStudent(UUID id, UUID schoolId, Student updatedData) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Student student = getStudentById(id, schoolId);

        if (updatedData.getFirstName() != null) student.setFirstName(updatedData.getFirstName());
        if (updatedData.getLastName() != null) student.setLastName(updatedData.getLastName());
        if (updatedData.getMiddleName() != null) student.setMiddleName(updatedData.getMiddleName());
        if (updatedData.getDateOfBirth() != null) student.setDateOfBirth(updatedData.getDateOfBirth());
        if (updatedData.getPlaceOfBirth() != null) student.setPlaceOfBirth(updatedData.getPlaceOfBirth());
        if (updatedData.getGender() != null) student.setGender(updatedData.getGender());
        if (updatedData.getPhotoUrl() != null) student.setPhotoUrl(updatedData.getPhotoUrl());
        if (updatedData.getStatus() != null) student.setStatus(updatedData.getStatus());

        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudentStatus(UUID id, UUID schoolId, StudentStatus status) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Student student = getStudentById(id, schoolId);
        student.setStatus(status);
        log.info("Updated student {} status to {}", student.getStudentNumber(), status);
        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public StudentBalanceResponse getStudentBalance(UUID studentId, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Student student = getStudentById(studentId, schoolId);

        BigDecimal totalDue = paymentScheduleRepository.sumStudentTotalExpectedAmount(schoolId, studentId);
        BigDecimal totalPaid = paymentScheduleRepository.sumStudentTotalCollectedAmount(schoolId, studentId);
        BigDecimal totalOutstanding = paymentScheduleRepository.sumStudentTotalOutstandingAmount(schoolId, studentId);

        return StudentBalanceResponse.builder()
                .studentId(student.getId())
                .studentNumber(student.getStudentNumber())
                .studentName(student.getFullName())
                .totalDue(totalDue)
                .totalPaid(totalPaid)
                .totalOutstanding(totalOutstanding)
                .currency(student.getSchool().getCurrency())
                .build();
    }

    @Transactional
    public void deleteStudent(UUID id, UUID schoolId) {
        Student student = getStudentById(id, schoolId);
        student.softDelete();
        studentRepository.save(student);
        log.info("Soft-deleted student {} from school {}", student.getStudentNumber(), schoolId);
    }
}

package com.izischool.parent.service;

import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentRelationship;
import com.izischool.parent.domain.ParentStatus;
import com.izischool.parent.domain.StudentParent;
import com.izischool.parent.repository.ParentRepository;
import com.izischool.parent.repository.StudentParentRepository;
import com.izischool.student.domain.Student;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public Parent createParent(Parent parent) {
        tenantValidationService.validateEntityAccess(parent);
        if (parent.getStatus() == null) {
            parent.setStatus(ParentStatus.ACTIVE);
        }
        return parentRepository.save(parent);
    }

    @Transactional
    public StudentParent linkStudentAndParent(
            Student student,
            Parent parent,
            ParentRelationship relationship,
            boolean isFinancialContact,
            boolean isPrimary
    ) {
        tenantValidationService.validateEntityAccess(student);
        tenantValidationService.validateEntityAccess(parent);

        return studentParentRepository.findByStudent_IdAndParent_Id(student.getId(), parent.getId())
                .orElseGet(() -> {
                    StudentParent link = StudentParent.builder()
                            .student(student)
                            .parent(parent)
                            .relationship(relationship)
                            .isFinancialContact(isFinancialContact)
                            .isPrimary(isPrimary)
                            .build();
                    StudentParent saved = studentParentRepository.save(link);
                    log.info("Linked student {} to parent {} (relationship={}, isFinancialContact={})",
                            student.getStudentNumber(), parent.getFullName(), relationship, isFinancialContact);
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public Parent getParentById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return parentRepository.findByIdAndSchool_IdAndDeletedFalse(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", id));
    }

    @Transactional(readOnly = true)
    public Page<Parent> getParentsBySchool(UUID schoolId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return parentRepository.findBySchool_IdAndDeletedFalse(schoolId, pageable);
    }

    @Transactional(readOnly = true)
    public List<StudentParent> getParentsForStudent(UUID studentId, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return studentParentRepository.findByStudent_Id(studentId);
    }
}

package com.izischool.parent.service;

import com.izischool.common.exception.ConflictException;
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
import java.util.stream.Collectors;

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
        UUID schoolId = parent.getSchool().getId();

        if (parent.getPhone() != null && !parent.getPhone().isBlank()) {
            if (parentRepository.existsBySchool_IdAndPhoneAndDeletedFalse(schoolId, parent.getPhone())) {
                throw new ConflictException(String.format("Parent with phone [%s] already exists in this school", parent.getPhone()));
            }
        }

        if (parent.getEmail() != null && !parent.getEmail().isBlank()) {
            if (parentRepository.existsBySchool_IdAndEmailAndDeletedFalse(schoolId, parent.getEmail())) {
                throw new ConflictException(String.format("Parent with email [%s] already exists in this school", parent.getEmail()));
            }
        }

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
            boolean isEmergencyContact
    ) {
        tenantValidationService.validateEntityAccess(student);
        tenantValidationService.validateEntityAccess(parent);

        if (studentParentRepository.existsByStudent_IdAndParent_Id(student.getId(), parent.getId())) {
            throw new ConflictException(String.format("Parent [%s] is already linked to student [%s]",
                    parent.getFullName(), student.getFullName()));
        }

        StudentParent link = StudentParent.builder()
                .student(student)
                .parent(parent)
                .relationship(relationship)
                .isFinancialContact(isFinancialContact)
                .isEmergencyContact(isEmergencyContact)
                .build();

        return studentParentRepository.save(link);
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
    public List<Student> getStudentsForParent(UUID parentId, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        List<StudentParent> links = studentParentRepository.findByParent_Id(parentId);
        return links.stream()
                .map(StudentParent::getStudent)
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toList());
    }

    @Transactional
    public Parent updateParent(UUID id, UUID schoolId, Parent updatedData) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Parent parent = getParentById(id, schoolId);

        if (updatedData.getFirstName() != null) parent.setFirstName(updatedData.getFirstName());
        if (updatedData.getLastName() != null) parent.setLastName(updatedData.getLastName());
        if (updatedData.getPhone() != null) parent.setPhone(updatedData.getPhone());
        if (updatedData.getWhatsappPhone() != null) parent.setWhatsappPhone(updatedData.getWhatsappPhone());
        if (updatedData.getEmail() != null) parent.setEmail(updatedData.getEmail());
        if (updatedData.getAddress() != null) parent.setAddress(updatedData.getAddress());
        if (updatedData.getStatus() != null) parent.setStatus(updatedData.getStatus());

        return parentRepository.save(parent);
    }
}

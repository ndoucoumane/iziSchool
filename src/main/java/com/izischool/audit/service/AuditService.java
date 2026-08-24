package com.izischool.audit.service;

import com.izischool.audit.domain.AuditAction;
import com.izischool.audit.domain.AuditLog;
import com.izischool.audit.repository.AuditLogRepository;
import com.izischool.school.domain.School;
import com.izischool.tenant.context.TenantContext;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(School school, String userId, AuditAction action, String entityType,
                          String entityId, String oldValue, String newValue, String ipAddress, String userAgent) {
        String effectiveUserId = userId != null ? userId :
                (TenantContext.getCurrentKeycloakUserId() != null ? TenantContext.getCurrentKeycloakUserId() : "SYSTEM");

        AuditLog logEntry = AuditLog.builder()
                .school(school)
                .userId(effectiveUserId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(logEntry);
        log.debug("Audit logged: [{}] {} on {} ({}) by user {}", action, entityType, entityId,
                school != null ? school.getCode() : "GLOBAL", effectiveUserId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsBySchool(UUID schoolId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return auditLogRepository.findBySchool_IdOrderByCreatedAtDesc(schoolId, pageable);
    }
}

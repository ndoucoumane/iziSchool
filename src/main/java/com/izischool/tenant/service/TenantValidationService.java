package com.izischool.tenant.service;

import com.izischool.common.entity.TenantAwareEntity;
import com.izischool.common.exception.TenantAccessException;
import com.izischool.school.domain.School;
import com.izischool.tenant.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class TenantValidationService {

    /**
     * Validates that the specified schoolId matches the current tenant context.
     * SUPER_ADMIN is allowed to bypass tenant checks.
     */
    public void validateSchoolAccess(UUID schoolId) {
        if (schoolId == null) {
            throw new TenantAccessException("Target school ID cannot be null");
        }

        TenantContext context = TenantContext.get();
        if (context == null) {
            // When running in background jobs/tests without request context or when context not yet initialized
            return;
        }

        if (context.isSuperAdmin()) {
            return;
        }

        if (context.getSchoolId() == null || !context.getSchoolId().equals(schoolId)) {
            log.warn("Security Alert: Tenant mismatch! User school: {}, Target school: {}",
                    context.getSchoolId(), schoolId);
            throw new TenantAccessException(String.format("Access denied: You do not have access to school [%s]", schoolId));
        }
    }

    /**
     * Validates that the specified School entity matches the current tenant context.
     */
    public void validateSchoolAccess(School school) {
        if (school == null) {
            throw new TenantAccessException("Target school cannot be null");
        }
        validateSchoolAccess(school.getId());
    }

    /**
     * Validates that a tenant-aware entity belongs to the current tenant context.
     */
    public void validateEntityAccess(TenantAwareEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getSchool() != null) {
            validateSchoolAccess(entity.getSchool().getId());
        }
    }
}

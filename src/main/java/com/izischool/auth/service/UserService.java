package com.izischool.auth.service;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.domain.UserStatus;
import com.izischool.auth.repository.UserProfileRepository;
import com.izischool.common.exception.ConflictException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public UserProfile createUserProfile(UserProfile userProfile) {
        if (userProfileRepository.existsByKeycloakUserId(userProfile.getKeycloakUserId())) {
            throw new ConflictException("User profile with this Keycloak ID already exists");
        }
        if (userProfile.getEmail() != null && userProfileRepository.existsByEmail(userProfile.getEmail())) {
            throw new ConflictException(String.format("User profile with email [%s] already exists", userProfile.getEmail()));
        }
        if (userProfile.getStatus() == null) {
            userProfile.setStatus(UserStatus.ACTIVE);
        }
        if (userProfile.getSchool() != null) {
            tenantValidationService.validateSchoolAccess(userProfile.getSchool().getId());
        }
        return userProfileRepository.save(userProfile);
    }

    @Transactional(readOnly = true)
    public UserProfile getUserProfileById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return userProfileRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", id));
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> findByKeycloakUserId(String keycloakUserId) {
        return userProfileRepository.findByKeycloakUserId(keycloakUserId);
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getUsersBySchool(UUID schoolId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return userProfileRepository.findBySchool_IdAndDeletedFalse(schoolId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getUsersBySchoolAndRole(UUID schoolId, UserRole role, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return userProfileRepository.findBySchool_IdAndRoleAndDeletedFalse(schoolId, role, pageable);
    }
}

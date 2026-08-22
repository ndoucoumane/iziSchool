package com.izischool.auth.service;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.repository.UserProfileRepository;
import com.izischool.common.exception.UnauthorizedException;
import com.izischool.school.domain.School;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserContextService {

    private final UserProfileRepository userProfileRepository;

    @Value("${izischool.security.auth-enabled:false}")
    private boolean authEnabled;

    @Value("${izischool.dev-seed.keycloak-user-id:dev-default-admin-id}")
    private String defaultKeycloakUserId;

    public Optional<String> getCurrentKeycloakUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return Optional.ofNullable(jwt.getSubject());
        }
        if (!authEnabled) {
            return Optional.of(defaultKeycloakUserId);
        }
        return Optional.empty();
    }

    public Optional<UserProfile> getCurrentUserProfile() {
        return getCurrentKeycloakUserId().flatMap(userProfileRepository::findByKeycloakUserId);
    }

    public UserProfile getRequiredCurrentUserProfile() {
        return getCurrentUserProfile()
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found"));
    }

    public Optional<School> getCurrentSchool() {
        return getCurrentUserProfile().map(UserProfile::getSchool);
    }

    public UUID getRequiredSchoolId() {
        UserProfile profile = getRequiredCurrentUserProfile();
        if (profile.getSchool() == null) {
            if (profile.getRole() == UserRole.SUPER_ADMIN) {
                return null;
            }
            throw new UnauthorizedException("User is not associated with any school tenant");
        }
        return profile.getSchool().getId();
    }
}

package com.izischool.tenant.filter;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.repository.UserProfileRepository;
import com.izischool.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final UserProfileRepository userProfileRepository;

    @Value("${izischool.security.auth-enabled:false}")
    private boolean authEnabled;

    @Value("${izischool.dev-seed.keycloak-user-id:dev-default-admin-id}")
    private String defaultKeycloakUserId;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String keycloakUserId = jwt.getSubject();
                String email = jwt.getClaimAsString("email");

                Set<String> roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                boolean isSuperAdmin = roles.contains("ROLE_SUPER_ADMIN");

                Optional<UserProfile> userProfileOpt = userProfileRepository.findByKeycloakUserId(keycloakUserId);

                TenantContext.TenantContextBuilder contextBuilder = TenantContext.builder()
                        .keycloakUserId(keycloakUserId)
                        .email(email)
                        .roles(roles)
                        .superAdmin(isSuperAdmin);

                userProfileOpt.ifPresent(profile -> {
                    if (profile.getSchool() != null) {
                        contextBuilder.schoolId(profile.getSchool().getId());
                        contextBuilder.schoolCode(profile.getSchool().getCode());
                    }
                    if (profile.getRole() == UserRole.SUPER_ADMIN) {
                        contextBuilder.superAdmin(true);
                    }
                });

                TenantContext.set(contextBuilder.build());
            } else if (!authEnabled) {
                // Development mode: Inject default admin user context automatically
                userProfileRepository.findByKeycloakUserId(defaultKeycloakUserId).ifPresentOrElse(profile -> {
                    TenantContext.set(TenantContext.builder()
                            .keycloakUserId(profile.getKeycloakUserId())
                            .email(profile.getEmail())
                            .roles(Set.of("ROLE_SUPER_ADMIN", "ROLE_DIRECTOR", "ROLE_ADMIN"))
                            .superAdmin(true)
                            .schoolId(profile.getSchool() != null ? profile.getSchool().getId() : null)
                            .schoolCode(profile.getSchool() != null ? profile.getSchool().getCode() : null)
                            .build());
                }, () -> {
                    TenantContext.set(TenantContext.builder()
                            .keycloakUserId("dev-admin")
                            .email("admin@izischool.com")
                            .roles(Set.of("ROLE_SUPER_ADMIN", "ROLE_DIRECTOR", "ROLE_ADMIN"))
                            .superAdmin(true)
                            .build());
                });
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

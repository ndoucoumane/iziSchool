package com.izischool.auth;

import com.izischool.auth.controller.AuthController;
import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.domain.UserStatus;
import com.izischool.auth.dto.AuthResponse;
import com.izischool.auth.dto.ChangePasswordRequest;
import com.izischool.auth.dto.KeycloakTokenResponse;
import com.izischool.auth.dto.LoginRequest;
import com.izischool.auth.dto.RefreshTokenRequest;
import com.izischool.auth.repository.UserProfileRepository;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.auth.service.KeycloakAuthService;
import com.izischool.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private CurrentUserContextService currentUserContextService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private KeycloakAuthService keycloakAuthService;

    @InjectMocks
    private AuthController authController;

    private UserProfile sampleProfile;
    private KeycloakTokenResponse sampleTokenResponse;

    @BeforeEach
    void setUp() {
        sampleProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .keycloakUserId("kc-superadmin-01")
                .email("super-admin@izischool.com")
                .firstName("Mor")
                .lastName("Ndao")
                .role(UserRole.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        sampleTokenResponse = KeycloakTokenResponse.builder()
                .accessToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.real-keycloak-token")
                .refreshToken("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.real-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("super-admin@izischool.com")
                .password("MonMotDePasse123!")
                .build();

        when(keycloakAuthService.login("super-admin@izischool.com", "MonMotDePasse123!"))
                .thenReturn(sampleTokenResponse);
        when(userProfileRepository.findByEmail("super-admin@izischool.com"))
                .thenReturn(Optional.of(sampleProfile));

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.real-keycloak-token", response.getBody().getAccessToken());
        assertEquals("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.real-refresh-token", response.getBody().getRefreshToken());
        assertEquals(3600, response.getBody().getExpiresIn());
        assertEquals("super-admin@izischool.com", response.getBody().getUser().getEmail());
    }

    @Test
    void testLoginInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("super-admin@izischool.com")
                .password("WrongPassword")
                .build();

        when(keycloakAuthService.login("super-admin@izischool.com", "WrongPassword"))
                .thenThrow(new UnauthorizedException("Email ou mot de passe invalide"));

        assertThrows(UnauthorizedException.class, () -> authController.login(request));
    }

    @Test
    void testLoginInactiveUser() {
        sampleProfile.setStatus(UserStatus.SUSPENDED);
        LoginRequest request = LoginRequest.builder()
                .email("super-admin@izischool.com")
                .password("MonMotDePasse123!")
                .build();

        when(keycloakAuthService.login("super-admin@izischool.com", "MonMotDePasse123!"))
                .thenReturn(sampleTokenResponse);
        when(userProfileRepository.findByEmail("super-admin@izischool.com"))
                .thenReturn(Optional.of(sampleProfile));

        assertThrows(UnauthorizedException.class, () -> authController.login(request));
    }

    @Test
    void testRefreshToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        KeycloakTokenResponse refreshed = KeycloakTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(3600)
                .build();

        when(keycloakAuthService.refreshToken("valid-refresh-token")).thenReturn(refreshed);
        when(currentUserContextService.getCurrentUserProfile()).thenReturn(Optional.of(sampleProfile));

        ResponseEntity<AuthResponse> response = authController.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getBody().getAccessToken());
        assertEquals("new-refresh-token", response.getBody().getRefreshToken());
    }

    @Test
    void testLogout() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("my-refresh-token")
                .build();

        ResponseEntity<Map<String, String>> response = authController.logout(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(keycloakAuthService, times(1)).logout("my-refresh-token");
    }

    @Test
    void testChangePassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewPass123!")
                .build();

        when(currentUserContextService.getRequiredCurrentUserProfile()).thenReturn(sampleProfile);

        ResponseEntity<Map<String, String>> response = authController.changePassword(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(keycloakAuthService, times(1)).changePassword(
                "super-admin@izischool.com",
                "OldPass123!",
                "NewPass123!",
                "kc-superadmin-01"
        );
    }
}

package com.izischool.auth.controller;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserStatus;
import com.izischool.auth.dto.AuthResponse;
import com.izischool.auth.dto.ChangePasswordRequest;
import com.izischool.auth.dto.KeycloakTokenResponse;
import com.izischool.auth.dto.LoginRequest;
import com.izischool.auth.dto.RefreshTokenRequest;
import com.izischool.auth.dto.UserProfileResponse;
import com.izischool.auth.repository.UserProfileRepository;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.auth.service.KeycloakAuthService;
import com.izischool.common.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints d'authentification, gestion des sessions et profil utilisateur")
public class AuthController {

    private final CurrentUserContextService currentUserContextService;
    private final UserProfileRepository userProfileRepository;
    private final KeycloakAuthService keycloakAuthService;

    @Operation(summary = "Connexion utilisateur", description = "Authentifie un utilisateur auprès de Keycloak et retourne ses tokens d'accès JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides ou serveur d'authentification injoignable"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides ou compte inactif")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Tentative de connexion pour l'email: {}", request.getEmail());

        // 1. Validation des identifiants et récupération des tokens réels auprès de Keycloak
        KeycloakTokenResponse tokenResponse = keycloakAuthService.login(request.getEmail(), request.getPassword());

        // 2. Recherche du profil local associé
        UserProfile profile = userProfileRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Profil utilisateur introuvable pour cet email"));

        // 3. Vérification du statut du compte
        if (profile.getStatus() != UserStatus.ACTIVE) {
            log.warn("Tentative de connexion sur un compte non actif: {} (statut={})", profile.getEmail(), profile.getStatus());
            throw new UnauthorizedException("Le compte utilisateur est inactif ou suspendu");
        }

        AuthResponse response = AuthResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenType(tokenResponse.getTokenType() != null ? tokenResponse.getTokenType() : "Bearer")
                .expiresIn(tokenResponse.getExpiresIn())
                .user(UserProfileResponse.fromEntity(profile))
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Rafraîchir le token d'accès", description = "Génère un nouveau token d'accès auprès de Keycloak à partir d'un refresh token valide")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token rafraîchi avec succès"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        KeycloakTokenResponse tokenResponse = keycloakAuthService.refreshToken(request.getRefreshToken());
        UserProfile current = currentUserContextService.getCurrentUserProfile().orElse(null);

        AuthResponse response = AuthResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken() != null ? tokenResponse.getRefreshToken() : request.getRefreshToken())
                .tokenType(tokenResponse.getTokenType() != null ? tokenResponse.getTokenType() : "Bearer")
                .expiresIn(tokenResponse.getExpiresIn())
                .user(current != null ? UserProfileResponse.fromEntity(current) : null)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Déconnexion utilisateur", description = "Invalide la session auprès de Keycloak")
    @ApiResponse(responseCode = "200", description = "Déconnexion réussie")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            keycloakAuthService.logout(request.getRefreshToken());
        }
        log.info("Utilisateur déconnecté");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @Operation(summary = "Profil utilisateur connecté", description = "Retourne les informations du compte utilisateur actuellement connecté",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil récupéré avec succès"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        UserProfile profile = currentUserContextService.getRequiredCurrentUserProfile();
        return ResponseEntity.ok(UserProfileResponse.fromEntity(profile));
    }

    @Operation(summary = "Modifier le mot de passe", description = "Met à jour le mot de passe du compte connecté dans Keycloak",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe modifié avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou mot de passe actuel incorrect"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserProfile profile = currentUserContextService.getRequiredCurrentUserProfile();
        keycloakAuthService.changePassword(
                profile.getEmail(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                profile.getKeycloakUserId()
        );
        log.info("Mot de passe mis à jour pour l'utilisateur: {}", profile.getEmail());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}

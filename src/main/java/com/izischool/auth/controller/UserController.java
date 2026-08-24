package com.izischool.auth.controller;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.dto.CreateUserRequest;
import com.izischool.auth.dto.UserProfileResponse;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.auth.service.UserService;
import com.izischool.common.response.PageResponse;
import com.izischool.school.domain.School;
import com.izischool.school.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des utilisateurs et des profils selon les rôles")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;
    private final SchoolService schoolService;
    private final CurrentUserContextService currentUserContextService;
    private final com.izischool.auth.service.KeycloakAdminService keycloakAdminService;

    @Operation(summary = "Lister les utilisateurs", description = "Retourne la liste paginée des utilisateurs de l'école (ou de la plateforme)")
    @ApiResponse(responseCode = "200", description = "Liste paginée des utilisateurs")
    @GetMapping
    public ResponseEntity<PageResponse<UserProfileResponse>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Page<UserProfile> usersPage = (role != null)
                ? userService.getUsersBySchoolAndRole(schoolId, role, PageRequest.of(page, size, Sort.by("lastName").ascending()))
                : userService.getUsersBySchool(schoolId, PageRequest.of(page, size, Sort.by("lastName").ascending()));

        return ResponseEntity.ok(PageResponse.of(usersPage, UserProfileResponse::fromEntity));
    }

    @Operation(summary = "Créer / Inscrire un utilisateur", description = "Crée un nouvel utilisateur avec son mot de passe et son rôle (SUPER_ADMIN, DIRECTOR, ADMIN, ACCOUNTANT, PARENT)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Email ou identifiant déjà existant")
    })
    @PostMapping({"", "/register"})
    public ResponseEntity<UserProfileResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        School school = null;
        if (request.getSchoolId() != null) {
            school = schoolService.getSchoolById(request.getSchoolId());
        } else if (request.getRole() != UserRole.SUPER_ADMIN) {
            UUID schoolId = currentUserContextService.getRequiredSchoolId();
            school = schoolService.getSchoolById(schoolId);
        }

        String keycloakId = (request.getKeycloakUserId() != null && !request.getKeycloakUserId().isBlank())
                ? request.getKeycloakUserId()
                : keycloakAdminService.createKeycloakUser(
                        request.getEmail(),
                        request.getPassword(),
                        request.getFirstName(),
                        request.getLastName(),
                        request.getRole()
                );

        UserProfile userProfile = request.toEntity(school);
        userProfile.setKeycloakUserId(keycloakId);

        UserProfile created = userService.createUserProfile(userProfile);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserProfileResponse.fromEntity(created));
    }

    @Operation(summary = "Détails d'un utilisateur", description = "Récupère les informations d'un profil utilisateur par son ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        UserProfile user = userService.getUserProfileById(id, schoolId);
        return ResponseEntity.ok(UserProfileResponse.fromEntity(user));
    }
}

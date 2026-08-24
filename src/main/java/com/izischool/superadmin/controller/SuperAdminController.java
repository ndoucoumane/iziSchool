package com.izischool.superadmin.controller;

import com.izischool.auth.domain.UserRole;
import com.izischool.auth.dto.CreateUserRequest;
import com.izischool.auth.dto.UserProfileResponse;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.exception.ForbiddenException;
import com.izischool.common.response.PageResponse;
import com.izischool.school.domain.School;
import com.izischool.school.dto.SchoolRequest;
import com.izischool.school.dto.SchoolResponse;
import com.izischool.school.dto.UpdateSchoolStatusRequest;
import com.izischool.school.service.SchoolService;
import com.izischool.superadmin.dto.CreateSchoolWithAdminRequest;
import com.izischool.superadmin.dto.ResetUserPasswordRequest;
import com.izischool.superadmin.dto.SuperAdminStatsResponse;
import com.izischool.superadmin.dto.UpdateUserStatusRequest;
import com.izischool.superadmin.service.SuperAdminService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Administration globale de la plateforme SaaS iziSchool (Établissements, Comptes Admin, Statistiques)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final SchoolService schoolService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Statistiques globales SaaS", description = "Indicateurs globaux de la plateforme (écoles, effectifs, revenus)")
    @ApiResponse(responseCode = "200", description = "Statistiques de la plateforme")
    @GetMapping("/stats")
    public ResponseEntity<SuperAdminStatsResponse> getPlatformStats() {
        validateSuperAdmin();
        return ResponseEntity.ok(superAdminService.getPlatformStats());
    }

    @Operation(summary = "Lister les établissements", description = "Retourne la liste paginée de tous les établissements clients")
    @ApiResponse(responseCode = "200", description = "Liste paginée des écoles")
    @GetMapping("/schools")
    public ResponseEntity<PageResponse<SchoolResponse>> getSchools(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validateSuperAdmin();
        Page<School> schoolPage = schoolService.getAllSchools(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.of(schoolPage, SchoolResponse::fromEntity));
    }

    @Operation(summary = "Créer un établissement", description = "Crée un nouvel établissement avec optionnellement son compte Directeur initial")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Établissement créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Code d'école ou email déjà existant")
    })
    @PostMapping("/schools")
    public ResponseEntity<SchoolResponse> createSchool(@Valid @RequestBody CreateSchoolWithAdminRequest request) {
        validateSuperAdmin();
        SchoolResponse response = superAdminService.createSchoolWithAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Détails d'un établissement", description = "Récupère les informations d'un établissement")
    @ApiResponse(responseCode = "200", description = "Détails de l'école")
    @GetMapping("/schools/{id}")
    public ResponseEntity<SchoolResponse> getSchoolById(@PathVariable UUID id) {
        validateSuperAdmin();
        School school = schoolService.getSchoolById(id);
        return ResponseEntity.ok(SchoolResponse.fromEntity(school));
    }

    @Operation(summary = "Modifier un établissement", description = "Met à jour les coordonnées et informations d'un établissement")
    @ApiResponse(responseCode = "200", description = "Établissement mis à jour")
    @PutMapping("/schools/{id}")
    public ResponseEntity<SchoolResponse> updateSchool(@PathVariable UUID id, @Valid @RequestBody SchoolRequest request) {
        validateSuperAdmin();
        School school = schoolService.getSchoolById(id);
        school.setName(request.getName());
        if (request.getCode() != null && !request.getCode().isBlank()) {
            school.setCode(request.getCode());
        }
        school.setEmail(request.getEmail());
        school.setPhone(request.getPhone());
        school.setAddress(request.getAddress());
        if (request.getCity() != null) school.setCity(request.getCity());
        if (request.getCountry() != null) school.setCountry(request.getCountry());
        if (request.getCurrency() != null) school.setCurrency(request.getCurrency());
        if (request.getLogoUrl() != null) school.setLogoUrl(request.getLogoUrl());

        School updated = schoolService.updateSchool(school);
        return ResponseEntity.ok(SchoolResponse.fromEntity(updated));
    }

    @Operation(summary = "Changer le statut d'un établissement", description = "Active, suspend ou désactive une école")
    @ApiResponse(responseCode = "200", description = "Statut mis à jour")
    @PatchMapping("/schools/{id}/status")
    public ResponseEntity<SchoolResponse> changeSchoolStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolStatusRequest request) {
        validateSuperAdmin();
        School updated = schoolService.updateSchoolStatus(id, request.getStatus());
        return ResponseEntity.ok(SchoolResponse.fromEntity(updated));
    }

    @Operation(summary = "Lister les administrateurs d'une école", description = "Retourne la liste des comptes Directeurs/Admins d'un établissement")
    @ApiResponse(responseCode = "200", description = "Liste des administrateurs")
    @GetMapping("/schools/{schoolId}/admins")
    public ResponseEntity<List<UserProfileResponse>> getSchoolAdmins(@PathVariable UUID schoolId) {
        validateSuperAdmin();
        return ResponseEntity.ok(superAdminService.getSchoolAdmins(schoolId));
    }

    @Operation(summary = "Créer un administrateur pour une école", description = "Crée un nouveau compte administratif pour une école donnée")
    @ApiResponse(responseCode = "201", description = "Compte administratif créé")
    @PostMapping("/schools/{schoolId}/admins")
    public ResponseEntity<UserProfileResponse> createSchoolAdmin(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateUserRequest request) {
        validateSuperAdmin();
        UserProfileResponse response = superAdminService.createSchoolAdmin(schoolId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Réinitialiser le mot de passe d'un utilisateur", description = "Modifie le mot de passe d'un utilisateur sur Keycloak")
    @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé")
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<Map<String, String>> resetUserPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ResetUserPasswordRequest request) {
        validateSuperAdmin();
        superAdminService.resetUserPassword(userId, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }

    @Operation(summary = "Changer le statut d'un utilisateur", description = "Active, suspend ou désactive un compte utilisateur")
    @ApiResponse(responseCode = "200", description = "Statut utilisateur mis à jour")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<UserProfileResponse> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        validateSuperAdmin();
        UserProfileResponse response = superAdminService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    private void validateSuperAdmin() {
        var profile = currentUserContextService.getRequiredCurrentUserProfile();
        if (profile.getRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("SUPER_ADMIN role required");
        }
    }
}

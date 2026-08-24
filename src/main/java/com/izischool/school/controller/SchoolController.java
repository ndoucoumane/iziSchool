package com.izischool.school.controller;

import com.izischool.auth.domain.UserRole;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.exception.ForbiddenException;
import com.izischool.common.response.PageResponse;
import com.izischool.school.domain.School;
import com.izischool.school.dto.SchoolRequest;
import com.izischool.school.dto.SchoolResponse;
import com.izischool.school.dto.UpdateSchoolStatusRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
@Tag(name = "Schools", description = "Gestion des établissements scolaires (Tenants)")
@SecurityRequirement(name = "BearerAuth")
public class SchoolController {

    private final SchoolService schoolService;
    private final com.izischool.superadmin.service.SuperAdminService superAdminService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Créer un établissement scolaire", description = "Accessible uniquement par le SUPER_ADMIN. Crée l'école et optionnellement le compte Directeur initial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "École créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "409", description = "Code d'établissement déjà utilisé")
    })
    @PostMapping
    public ResponseEntity<SchoolResponse> createSchool(@Valid @RequestBody SchoolRequest request) {
        validateSuperAdmin();
        com.izischool.superadmin.dto.CreateSchoolWithAdminRequest adminReq = com.izischool.superadmin.dto.CreateSchoolWithAdminRequest.builder()
                .name(request.getName())
                .code(request.getCode())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .currency(request.getCurrency())
                .logoUrl(request.getLogoUrl())
                .adminFirstName(request.getAdminFirstName())
                .adminLastName(request.getAdminLastName())
                .adminEmail(request.getAdminEmail())
                .adminPassword(request.getAdminPassword())
                .adminPhone(request.getAdminPhone())
                .build();
        SchoolResponse created = superAdminService.createSchoolWithAdmin(adminReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Lister les établissements scolaires", description = "Accessible uniquement par le SUPER_ADMIN avec pagination")
    @ApiResponse(responseCode = "200", description = "Liste paginée des établissements")
    @GetMapping
    public ResponseEntity<PageResponse<SchoolResponse>> getSchools(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validateSuperAdmin();
        Page<School> schoolPage = schoolService.getAllSchools(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.of(schoolPage, SchoolResponse::fromEntity));
    }

    @Operation(summary = "Détails d'un établissement", description = "Accessible par SUPER_ADMIN ou le personnel de l'école")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détails de l'établissement"),
            @ApiResponse(responseCode = "404", description = "Établissement non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SchoolResponse> getSchoolById(@PathVariable UUID id) {
        validateSchoolOrSuperAdmin(id);
        School school = schoolService.getSchoolById(id);
        return ResponseEntity.ok(SchoolResponse.fromEntity(school));
    }

    @Operation(summary = "Mettre à jour un établissement", description = "Modifie les informations générales de l'école")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Établissement mis à jour"),
            @ApiResponse(responseCode = "404", description = "Établissement non trouvé")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SchoolResponse> updateSchool(@PathVariable UUID id, @Valid @RequestBody SchoolRequest request) {
        validateSchoolOrSuperAdmin(id);
        School school = schoolService.getSchoolById(id);
        school.setName(request.getName());
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

    @Operation(summary = "Changer le statut d'un établissement", description = "Activer, désactiver ou suspendre une école (SUPER_ADMIN)")
    @ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès")
    @PatchMapping("/{id}/status")
    public ResponseEntity<SchoolResponse> changeSchoolStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolStatusRequest request) {
        validateSuperAdmin();
        School updated = schoolService.updateSchoolStatus(id, request.getStatus());
        return ResponseEntity.ok(SchoolResponse.fromEntity(updated));
    }

    private void validateSuperAdmin() {
        var profile = currentUserContextService.getRequiredCurrentUserProfile();
        if (profile.getRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("SUPER_ADMIN role required");
        }
    }

    private void validateSchoolOrSuperAdmin(UUID schoolId) {
        var profile = currentUserContextService.getRequiredCurrentUserProfile();
        if (profile.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (profile.getSchool() == null || !profile.getSchool().getId().equals(schoolId)) {
            throw new ForbiddenException("Access denied to another school tenant");
        }
    }
}

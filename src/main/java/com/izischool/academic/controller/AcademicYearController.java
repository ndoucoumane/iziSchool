package com.izischool.academic.controller;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.dto.AcademicYearRequest;
import com.izischool.academic.dto.AcademicYearResponse;
import com.izischool.academic.service.AcademicYearService;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.school.domain.School;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Years", description = "Gestion des années scolaires de l'établissement")
@SecurityRequirement(name = "BearerAuth")
public class AcademicYearController {

    private final AcademicYearService academicYearService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les années scolaires", description = "Retourne la liste ordonnée des années scolaires de l'école")
    @ApiResponse(responseCode = "200", description = "Liste des années scolaires")
    @GetMapping
    public ResponseEntity<List<AcademicYearResponse>> listAcademicYears() {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<AcademicYear> years = academicYearService.getAcademicYearsBySchool(schoolId);
        List<AcademicYearResponse> response = years.stream()
                .map(AcademicYearResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Récupérer l'année scolaire active", description = "Retourne l'année scolaire actuellement active de l'établissement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Année scolaire active trouvée"),
            @ApiResponse(responseCode = "404", description = "Aucune année active trouvée")
    })
    @GetMapping("/active")
    public ResponseEntity<AcademicYearResponse> getActiveAcademicYear() {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        return academicYearService.getActiveAcademicYear(schoolId)
                .map(AcademicYearResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Créer une année scolaire", description = "Crée une nouvelle année scolaire pour l'école connectée")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Année scolaire créée"),
            @ApiResponse(responseCode = "400", description = "Dates invalides"),
            @ApiResponse(responseCode = "409", description = "Année scolaire déjà existante")
    })
    @PostMapping
    public ResponseEntity<AcademicYearResponse> createAcademicYear(@Valid @RequestBody AcademicYearRequest request) {
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        AcademicYear created = academicYearService.createAcademicYear(request.toEntity(school));
        return ResponseEntity.status(HttpStatus.CREATED).body(AcademicYearResponse.fromEntity(created));
    }

    @Operation(summary = "Détails d'une année scolaire", description = "Récupère les informations d'une année scolaire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détails trouvés"),
            @ApiResponse(responseCode = "404", description = "Année scolaire non trouvée")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AcademicYearResponse> getAcademicYearById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        AcademicYear year = academicYearService.getAcademicYearById(id, schoolId);
        return ResponseEntity.ok(AcademicYearResponse.fromEntity(year));
    }

    @Operation(summary = "Modifier une année scolaire", description = "Met à jour le nom et les dates d'une année scolaire")
    @ApiResponse(responseCode = "200", description = "Année scolaire mise à jour")
    @PutMapping("/{id}")
    public ResponseEntity<AcademicYearResponse> updateAcademicYear(
            @PathVariable UUID id,
            @Valid @RequestBody AcademicYearRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        AcademicYear yearData = AcademicYear.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        AcademicYear updated = academicYearService.updateAcademicYear(id, schoolId, yearData);
        return ResponseEntity.ok(AcademicYearResponse.fromEntity(updated));
    }

    @Operation(summary = "Activer une année scolaire", description = "Active l'année scolaire spécifiée et clôture l'éventuelle année active précédente")
    @ApiResponse(responseCode = "200", description = "Année scolaire activée avec succès")
    @PostMapping("/{id}/activate")
    public ResponseEntity<AcademicYearResponse> activateAcademicYear(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        AcademicYear activated = academicYearService.activateAcademicYear(id, schoolId);
        return ResponseEntity.ok(AcademicYearResponse.fromEntity(activated));
    }

    @Operation(summary = "Clôturer une année scolaire", description = "Clôture l'année scolaire")
    @ApiResponse(responseCode = "200", description = "Année scolaire clôturée")
    @PostMapping("/{id}/close")
    public ResponseEntity<AcademicYearResponse> closeAcademicYear(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        AcademicYear closed = academicYearService.closeAcademicYear(id, schoolId);
        return ResponseEntity.ok(AcademicYearResponse.fromEntity(closed));
    }
}

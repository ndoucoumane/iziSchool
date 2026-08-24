package com.izischool.finance.controller;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.GradeLevel;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.service.AcademicYearService;
import com.izischool.academic.service.GradeLevelService;
import com.izischool.academic.service.SchoolClassService;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeAssignment;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.dto.FeeRequest;
import com.izischool.finance.dto.FeeResponse;
import com.izischool.finance.dto.UpdateFeeStatusRequest;
import com.izischool.finance.service.FeeService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
@Tag(name = "Fees", description = "Configuration des frais scolaires (scolarité, inscription, etc.)")
@SecurityRequirement(name = "BearerAuth")
public class FeeController {

    private final FeeService feeService;
    private final AcademicYearService academicYearService;
    private final SchoolClassService schoolClassService;
    private final GradeLevelService gradeLevelService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les frais scolaires", description = "Retourne la liste des frais scolaires avec filtres optionnels")
    @ApiResponse(responseCode = "200", description = "Liste des frais")
    @GetMapping
    public ResponseEntity<List<FeeResponse>> listFees(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) FeeType type,
            @RequestParam(required = false) Boolean status) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<Fee> fees = feeService.getFeesFiltered(schoolId, academicYearId, type, status);
        List<FeeResponse> response = fees.stream()
                .map(FeeResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Créer un frais scolaire", description = "Enregistre une nouvelle ligne de frais pour l'année scolaire")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Frais créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Code de frais déjà utilisé")
    })
    @PostMapping
    public ResponseEntity<FeeResponse> createFee(@Valid @RequestBody FeeRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        AcademicYear academicYear = academicYearService.getAcademicYearById(request.getAcademicYearId(), schoolId);
        Fee createdFee = feeService.createFee(request.toEntity(school, academicYear));

        // Create assignment if class or grade level is provided
        if (request.getClassId() != null || request.getGradeLevelId() != null) {
            SchoolClass schoolClass = request.getClassId() != null ? schoolClassService.getSchoolClassById(request.getClassId(), schoolId) : null;
            GradeLevel gradeLevel = request.getGradeLevelId() != null ? gradeLevelService.getGradeLevelById(request.getGradeLevelId(), schoolId) : null;

            FeeAssignment assignment = FeeAssignment.builder()
                    .school(school)
                    .academicYear(academicYear)
                    .fee(createdFee)
                    .schoolClass(schoolClass)
                    .gradeLevel(gradeLevel)
                    .amount(request.getAmount())
                    .active(true)
                    .build();
            feeService.createFeeAssignment(assignment);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(FeeResponse.fromEntity(createdFee));
    }

    @Operation(summary = "Détails d'un frais", description = "Récupère les détails d'un frais scolaire par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frais trouvé"),
            @ApiResponse(responseCode = "404", description = "Frais non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FeeResponse> getFeeById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Fee fee = feeService.getFeeById(id, schoolId);
        return ResponseEntity.ok(FeeResponse.fromEntity(fee));
    }

    @Operation(summary = "Modifier un frais", description = "Met à jour le nom, montant ou statut d'un frais")
    @ApiResponse(responseCode = "200", description = "Frais mis à jour")
    @PutMapping("/{id}")
    public ResponseEntity<FeeResponse> updateFee(
            @PathVariable UUID id,
            @Valid @RequestBody FeeRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Fee feeData = Fee.builder()
                .name(request.getName())
                .amount(request.getAmount())
                .description(request.getDescription())
                .feeType(request.getType())
                .mandatory(request.isMandatory())
                .build();

        Fee updated = feeService.updateFee(id, schoolId, feeData);
        return ResponseEntity.ok(FeeResponse.fromEntity(updated));
    }

    @Operation(summary = "Activer / Désactiver un frais", description = "Change la disponibilité active d'un frais")
    @ApiResponse(responseCode = "200", description = "Statut mis à jour")
    @PatchMapping("/{id}/status")
    public ResponseEntity<FeeResponse> toggleFeeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeeStatusRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Fee updated = feeService.toggleFeeStatus(id, schoolId, request.getActive());
        return ResponseEntity.ok(FeeResponse.fromEntity(updated));
    }
}

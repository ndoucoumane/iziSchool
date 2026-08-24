package com.izischool.academic.controller;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.GradeLevel;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import com.izischool.academic.dto.SchoolClassRequest;
import com.izischool.academic.dto.SchoolClassResponse;
import com.izischool.academic.service.AcademicYearService;
import com.izischool.academic.service.GradeLevelService;
import com.izischool.academic.service.SchoolClassService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "Gestion des classes et sections scolaires")
@SecurityRequirement(name = "BearerAuth")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;
    private final AcademicYearService academicYearService;
    private final GradeLevelService gradeLevelService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les classes", description = "Retourne la liste des classes avec filtres optionnels")
    @ApiResponse(responseCode = "200", description = "Liste des classes filtrées")
    @GetMapping
    public ResponseEntity<List<SchoolClassResponse>> listClasses(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) SchoolClassStatus status) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<SchoolClass> classes = schoolClassService.getClassesFiltered(schoolId, academicYearId, name, level, status);
        List<SchoolClassResponse> response = classes.stream()
                .map(SchoolClassResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Créer une classe", description = "Crée une classe associée à une année scolaire et un niveau")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Classe créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Code de classe déjà existant pour cette année")
    })
    @PostMapping
    public ResponseEntity<SchoolClassResponse> createClass(@Valid @RequestBody SchoolClassRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        AcademicYear academicYear = academicYearService.getAcademicYearById(request.getAcademicYearId(), schoolId);

        GradeLevel gradeLevel = null;
        if (request.getGradeLevelId() != null) {
            gradeLevel = gradeLevelService.getGradeLevelById(request.getGradeLevelId(), schoolId);
        } else if (request.getLevel() != null && !request.getLevel().isBlank()) {
            List<GradeLevel> grades = gradeLevelService.getActiveGradeLevels(schoolId);
            gradeLevel = grades.stream()
                    .filter(g -> g.getName().equalsIgnoreCase(request.getLevel()) || g.getCode().equalsIgnoreCase(request.getLevel()))
                    .findFirst()
                    .orElseGet(() -> gradeLevelService.createGradeLevel(GradeLevel.builder()
                            .school(school)
                            .name(request.getLevel())
                            .code(request.getLevel().toUpperCase().replaceAll("\\s+", ""))
                            .displayOrder(grades.size() + 1)
                            .active(true)
                            .build()));
        }

        String classCode = (request.getCode() != null && !request.getCode().isBlank())
                ? request.getCode()
                : request.getName().toUpperCase().replaceAll("\\s+", "-");

        SchoolClass schoolClass = SchoolClass.builder()
                .school(school)
                .academicYear(academicYear)
                .gradeLevel(gradeLevel)
                .name(request.getName())
                .code(classCode)
                .capacity(request.getCapacity() > 0 ? request.getCapacity() : 35)
                .status(SchoolClassStatus.ACTIVE)
                .build();

        SchoolClass created = schoolClassService.createSchoolClass(schoolClass);
        return ResponseEntity.status(HttpStatus.CREATED).body(SchoolClassResponse.fromEntity(created));
    }

    @Operation(summary = "Détails d'une classe", description = "Récupère les détails d'une classe par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détails de la classe"),
            @ApiResponse(responseCode = "404", description = "Classe non trouvée")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SchoolClassResponse> getClassById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        SchoolClass schoolClass = schoolClassService.getSchoolClassById(id, schoolId);
        return ResponseEntity.ok(SchoolClassResponse.fromEntity(schoolClass));
    }

    @Operation(summary = "Modifier une classe", description = "Met à jour le nom, la capacité ou le statut d'une classe")
    @ApiResponse(responseCode = "200", description = "Classe mise à jour")
    @PutMapping("/{id}")
    public ResponseEntity<SchoolClassResponse> updateClass(
            @PathVariable UUID id,
            @Valid @RequestBody SchoolClassRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        SchoolClass classData = SchoolClass.builder()
                .name(request.getName())
                .capacity(request.getCapacity())
                .build();
        SchoolClass updated = schoolClassService.updateSchoolClass(id, schoolId, classData);
        return ResponseEntity.ok(SchoolClassResponse.fromEntity(updated));
    }

    @Operation(summary = "Désactiver une classe", description = "Archive/désactive logiquement la classe (évite la suppression physique)")
    @ApiResponse(responseCode = "200", description = "Classe archivée")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteClass(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        schoolClassService.deactivateSchoolClass(id, schoolId);
        return ResponseEntity.ok(Map.of("message", "Class deactivated successfully"));
    }
}

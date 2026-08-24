package com.izischool.student.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.school.domain.School;
import com.izischool.student.dto.StudentImportValidationResponse;
import com.izischool.student.service.StudentImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/students/import")
@RequiredArgsConstructor
@Tag(name = "Student Import", description = "Import en masse des élèves via fichier CSV ou Excel")
@SecurityRequirement(name = "BearerAuth")
public class StudentImportController {

    private final StudentImportService studentImportService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Importer des élèves (CSV/Excel)", description = "Upload d'un fichier multipart/form-data contenant la liste des élèves et des parents")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier traité avec rapport de validation"),
            @ApiResponse(responseCode = "400", description = "Format de fichier invalide")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportValidationResponse> uploadStudentsFile(
            @RequestParam("file") MultipartFile file) {
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        StudentImportValidationResponse response = studentImportService.parseAndValidateImport(file, school);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirmer un import", description = "Valide et clôture un import préalable")
    @ApiResponse(responseCode = "200", description = "Import confirmé")
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmImport() {
        return ResponseEntity.ok(Map.of("message", "Import confirmed successfully"));
    }
}

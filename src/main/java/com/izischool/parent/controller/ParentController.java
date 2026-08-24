package com.izischool.parent.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.response.PageResponse;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentRelationship;
import com.izischool.parent.dto.ParentRequest;
import com.izischool.parent.dto.ParentResponse;
import com.izischool.parent.service.ParentService;
import com.izischool.school.domain.School;
import com.izischool.student.domain.Student;
import com.izischool.student.dto.StudentResponse;
import com.izischool.student.service.StudentService;
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
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
@Tag(name = "Parents", description = "Gestion des parents d'élèves et contacts financiers")
@SecurityRequirement(name = "BearerAuth")
public class ParentController {

    private final ParentService parentService;
    private final StudentService studentService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les parents", description = "Retourne la liste paginée des parents d'élèves de l'établissement")
    @ApiResponse(responseCode = "200", description = "Liste paginée des parents")
    @GetMapping
    public ResponseEntity<PageResponse<ParentResponse>> listParents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Page<Parent> parentPage = parentService.getParentsBySchool(schoolId, PageRequest.of(page, size, Sort.by("lastName").ascending()));
        return ResponseEntity.ok(PageResponse.of(parentPage, ParentResponse::fromEntity));
    }

    @Operation(summary = "Créer un parent", description = "Enregistre un nouveau parent d'élève")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parent créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Numéro de téléphone ou email déjà enregistré")
    })
    @PostMapping
    public ResponseEntity<ParentResponse> createParent(@Valid @RequestBody ParentRequest request) {
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        Parent parent = request.toEntity(school);
        Parent createdParent = parentService.createParent(parent);
        return ResponseEntity.status(HttpStatus.CREATED).body(ParentResponse.fromEntity(createdParent));
    }

    @Operation(summary = "Détails d'un parent", description = "Récupère les informations d'un parent par son ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parent trouvé"),
            @ApiResponse(responseCode = "404", description = "Parent non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ParentResponse> getParentById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Parent parent = parentService.getParentById(id, schoolId);
        return ResponseEntity.ok(ParentResponse.fromEntity(parent));
    }

    @Operation(summary = "Modifier un parent", description = "Met à jour les coordonnées d'un parent")
    @ApiResponse(responseCode = "200", description = "Parent mis à jour")
    @PutMapping("/{id}")
    public ResponseEntity<ParentResponse> updateParent(
            @PathVariable UUID id,
            @Valid @RequestBody ParentRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Parent parentData = Parent.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .whatsappPhone(request.getWhatsappPhone() != null ? request.getWhatsappPhone() : request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .build();

        Parent updated = parentService.updateParent(id, schoolId, parentData);
        return ResponseEntity.ok(ParentResponse.fromEntity(updated));
    }

    @Operation(summary = "Lister les enfants d'un parent", description = "Retourne tous les élèves rattachés à ce parent")
    @ApiResponse(responseCode = "200", description = "Liste des élèves du parent")
    @GetMapping("/{id}/students")
    public ResponseEntity<List<StudentResponse>> getStudentsForParent(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<Student> students = parentService.getStudentsForParent(id, schoolId);
        List<StudentResponse> response = students.stream()
                .map(StudentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Rattacher un élève à un parent", description = "Crée un lien de parenté entre un élève et un parent")
    @ApiResponse(responseCode = "200", description = "Lien créé avec succès")
    @PostMapping("/{parentId}/link-student/{studentId}")
    public ResponseEntity<Void> linkStudent(
            @PathVariable UUID parentId,
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "FATHER") ParentRelationship relationship,
            @RequestParam(defaultValue = "true") boolean isFinancialContact,
            @RequestParam(defaultValue = "false") boolean isEmergencyContact) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Student student = studentService.getStudentById(studentId, schoolId);
        Parent parent = parentService.getParentById(parentId, schoolId);

        parentService.linkStudentAndParent(student, parent, relationship, isFinancialContact, isEmergencyContact);
        return ResponseEntity.ok().build();
    }
}

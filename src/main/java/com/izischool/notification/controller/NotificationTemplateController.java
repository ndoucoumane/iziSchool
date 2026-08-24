package com.izischool.notification.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.notification.domain.NotificationTemplate;
import com.izischool.notification.dto.NotificationTemplateRequest;
import com.izischool.notification.dto.NotificationTemplateResponse;
import com.izischool.notification.repository.NotificationTemplateRepository;
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
@RequestMapping("/api/v1/notification-templates")
@RequiredArgsConstructor
@Tag(name = "Notification Templates", description = "Modèles de messages prédéfinis avec variables dynamiques")
@SecurityRequirement(name = "BearerAuth")
public class NotificationTemplateController {

    private final NotificationTemplateRepository templateRepository;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les modèles", description = "Retourne la liste des modèles de notification actifs de l'école")
    @ApiResponse(responseCode = "200", description = "Liste des modèles")
    @GetMapping
    public ResponseEntity<List<NotificationTemplateResponse>> listTemplates() {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<NotificationTemplate> templates = templateRepository.findBySchool_IdAndActiveTrue(schoolId);
        List<NotificationTemplateResponse> response = templates.stream()
                .map(NotificationTemplateResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Créer un modèle", description = "Enregistre un nouveau modèle de notification")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Modèle créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping
    public ResponseEntity<NotificationTemplateResponse> createTemplate(@Valid @RequestBody NotificationTemplateRequest request) {
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        NotificationTemplate template = request.toEntity(school);
        NotificationTemplate saved = templateRepository.save(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationTemplateResponse.fromEntity(saved));
    }

    @Operation(summary = "Modifier un modèle", description = "Met à jour le sujet ou corps du modèle")
    @ApiResponse(responseCode = "200", description = "Modèle mis à jour")
    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        NotificationTemplate template = templateRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id));

        template.setName(request.getName());
        template.setType(request.getType());
        template.setChannel(request.getChannel());
        template.setSubject(request.getSubject());
        template.setContent(request.getContent());
        template.setActive(request.isActive());

        NotificationTemplate updated = templateRepository.save(template);
        return ResponseEntity.ok(NotificationTemplateResponse.fromEntity(updated));
    }
}

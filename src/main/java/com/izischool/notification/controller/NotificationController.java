package com.izischool.notification.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.response.PageResponse;
import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationStatus;
import com.izischool.notification.domain.NotificationTemplate;
import com.izischool.notification.dto.NotificationResponse;
import com.izischool.notification.dto.SendNotificationRequest;
import com.izischool.notification.service.NotificationService;
import com.izischool.school.domain.School;
import com.izischool.student.domain.Student;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Envoi et historique des notifications (SMS, Email, WhatsApp)")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final StudentService studentService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les notifications", description = "Retourne l'historique paginé des notifications envoyées")
    @ApiResponse(responseCode = "200", description = "Liste paginée des notifications")
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Page<Notification> notificationPage = notificationService.getNotificationsBySchool(schoolId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.of(notificationPage, NotificationResponse::fromEntity));
    }

    @Operation(summary = "Envoyer une notification", description = "Envoie manuellement un rappel ou message à un destinataire (SMS/Email/WhatsApp)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification enregistrée et expédiée"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        Student student = request.getStudentId() != null ? studentService.getStudentById(request.getStudentId(), schoolId) : null;

        String body = request.getMessage();
        String title = request.getTitle() != null ? request.getTitle() : "Notification iziSchool";

        if ((body == null || body.isBlank()) && request.getType() != null) {
            Optional<NotificationTemplate> templateOpt = notificationService.findTemplate(schoolId, request.getType(), request.getChannel());
            if (templateOpt.isPresent()) {
                body = notificationService.formatMessage(templateOpt.get().getContent(), request.getVariables());
                if (templateOpt.get().getSubject() != null) {
                    title = notificationService.formatMessage(templateOpt.get().getSubject(), request.getVariables());
                }
            }
        }

        if (body == null || body.isBlank()) {
            body = "Notification de votre établissement scolaire iziSchool.";
        }

        Notification notification = Notification.builder()
                .school(school)
                .student(student)
                .recipientPhone(request.getRecipientPhone())
                .recipientEmail(request.getRecipientEmail())
                .type(request.getType())
                .channel(request.getChannel())
                .status(NotificationStatus.PENDING)
                .title(title)
                .message(body)
                .build();

        Notification created = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.fromEntity(created));
    }
}

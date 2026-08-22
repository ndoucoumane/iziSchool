package com.izischool.notification.service;

import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationChannel;
import com.izischool.notification.domain.NotificationStatus;
import com.izischool.notification.domain.NotificationTemplate;
import com.izischool.notification.domain.NotificationType;
import com.izischool.notification.provider.NotificationProvider;
import com.izischool.notification.repository.NotificationRepository;
import com.izischool.notification.repository.NotificationTemplateRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final List<NotificationProvider> providers;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public Notification createNotification(Notification notification) {
        tenantValidationService.validateEntityAccess(notification);
        if (notification.getStatus() == null) {
            notification.setStatus(NotificationStatus.PENDING);
        }
        Notification saved = notificationRepository.save(notification);
        dispatchNotification(saved);
        return saved;
    }

    public void dispatchNotification(Notification notification) {
        Optional<NotificationProvider> providerOpt = providers.stream()
                .filter(p -> p.getChannel() == notification.getChannel())
                .findFirst();

        if (providerOpt.isPresent()) {
            try {
                notification.setStatus(NotificationStatus.PROCESSING);
                boolean success = providerOpt.get().send(notification);
                if (success) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(Instant.now());
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setFailureReason("Provider returned failure response");
                }
            } catch (Exception e) {
                log.error("Failed to send notification {}: ", notification.getId(), e);
                notification.setStatus(NotificationStatus.FAILED);
                notification.setFailureReason(e.getMessage());
            }
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason("No provider available for channel " + notification.getChannel());
        }

        notificationRepository.save(notification);
    }

    public String formatMessage(String template, Map<String, String> variables) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> findTemplate(UUID schoolId, NotificationType type, NotificationChannel channel) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return templateRepository.findBySchool_IdAndTypeAndChannelAndActiveTrue(schoolId, type, channel);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsBySchool(UUID schoolId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return notificationRepository.findBySchool_IdOrderByCreatedAtDesc(schoolId, pageable);
    }
}

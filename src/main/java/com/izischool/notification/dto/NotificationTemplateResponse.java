package com.izischool.notification.dto;

import com.izischool.notification.domain.NotificationChannel;
import com.izischool.notification.domain.NotificationTemplate;
import com.izischool.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateResponse {

    private UUID id;
    private String name;
    private NotificationType type;
    private NotificationChannel channel;
    private String subject;
    private String content;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static NotificationTemplateResponse fromEntity(NotificationTemplate template) {
        if (template == null) {
            return null;
        }
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .type(template.getType())
                .channel(template.getChannel())
                .subject(template.getSubject())
                .content(template.getContent())
                .active(template.isActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}

package com.izischool.notification.dto;

import com.izischool.notification.domain.NotificationChannel;
import com.izischool.notification.domain.NotificationTemplate;
import com.izischool.notification.domain.NotificationType;
import com.izischool.school.domain.School;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @Size(max = 255)
    private String subject;

    @NotBlank(message = "Content template is required")
    private String content;

    @Builder.Default
    private boolean active = true;

    public NotificationTemplate toEntity(School school) {
        return NotificationTemplate.builder()
                .school(school)
                .name(name)
                .type(type)
                .channel(channel)
                .subject(subject)
                .content(content)
                .active(active)
                .build();
    }
}

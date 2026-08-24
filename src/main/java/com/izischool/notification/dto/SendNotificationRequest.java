package com.izischool.notification.dto;

import com.izischool.notification.domain.NotificationChannel;
import com.izischool.notification.domain.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    private UUID recipientId;

    private UUID studentId;

    private String recipientPhone;

    private String recipientEmail;

    @Builder.Default
    @NotNull(message = "Channel is required (SMS, EMAIL, WHATSAPP)")
    private NotificationChannel channel = NotificationChannel.SMS;

    @Builder.Default
    @NotNull(message = "Notification type is required")
    private NotificationType type = NotificationType.DUE_DATE_REMINDER;

    private String title;

    private String message;

    @Builder.Default
    private Map<String, String> variables = new HashMap<>();
}

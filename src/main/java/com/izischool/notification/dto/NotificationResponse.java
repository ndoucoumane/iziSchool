package com.izischool.notification.dto;

import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationChannel;
import com.izischool.notification.domain.NotificationStatus;
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
public class NotificationResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String recipientPhone;
    private String recipientEmail;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;
    private String title;
    private String message;
    private Instant sentAt;
    private String failureReason;
    private Instant createdAt;

    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .studentId(notification.getStudent() != null ? notification.getStudent().getId() : null)
                .studentName(notification.getStudent() != null ? notification.getStudent().getFullName() : null)
                .recipientPhone(notification.getRecipientPhone())
                .recipientEmail(notification.getRecipientEmail())
                .type(notification.getType())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .sentAt(notification.getSentAt())
                .failureReason(notification.getFailureReason())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

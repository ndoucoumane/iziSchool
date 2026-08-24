package com.izischool.notification.provider;

import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WhatsAppNotificationProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("Sending WhatsApp message to {}: {}", notification.getRecipientPhone(), notification.getMessage());
        return true;
    }
}

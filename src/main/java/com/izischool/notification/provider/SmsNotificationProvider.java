package com.izischool.notification.provider;

import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotificationProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("Sending SMS to {}: {}", notification.getRecipientPhone(), notification.getMessage());
        return true;
    }
}

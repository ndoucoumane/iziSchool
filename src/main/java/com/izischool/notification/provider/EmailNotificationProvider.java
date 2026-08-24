package com.izischool.notification.provider;

import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("Sending Email to {}: Subject [{}], Body [{}]",
                notification.getRecipientEmail(), notification.getTitle(), notification.getMessage());
        return true;
    }
}

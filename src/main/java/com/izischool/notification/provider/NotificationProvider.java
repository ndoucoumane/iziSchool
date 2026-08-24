package com.izischool.notification.provider;

import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationChannel;

public interface NotificationProvider {

    NotificationChannel getChannel();

    boolean send(Notification notification);
}

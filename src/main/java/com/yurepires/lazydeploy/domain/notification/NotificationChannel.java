package com.yurepires.lazydeploy.domain.notification;

public interface NotificationChannel {
    String type();

    NotificationResult send(NotificationCandidate candidate, NotificationChannelConfiguration configuration);
}

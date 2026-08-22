package com.yurepires.lazydeploy.model.notification;

public interface NotificationChannel {

    String type();

    NotificationResult send(NotificationCandidate candidate, NotificationChannelConfiguration configuration);

}

package com.yurepires.lazydeploy.domain.notification;

import java.time.Instant;

public record NotificationResult(
        boolean success,
        String channelType,
        Instant sentAt,
        String errorMessage
) {
    public static NotificationResult success(String channelType, Instant sentAt) {
        return new NotificationResult(true, channelType, sentAt, null);
    }

    public static NotificationResult failure(String channelType, String message) {
        return new NotificationResult(false, channelType, null, message);
    }
}

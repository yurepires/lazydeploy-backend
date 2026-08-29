package com.yurepires.lazydeploy.model.notification;

import java.time.Instant;

public record NotificationResult(
        boolean success,
        String channelType,
        Instant sentAt,
        String errorMessage,
        String recipientSnapshot
) {
    public NotificationResult(
            boolean success,
            String channelType,
            Instant sentAt,
            String errorMessage
    ) {
        this(success, channelType, sentAt, errorMessage, null);
    }

    public static NotificationResult success(String channelType, Instant sentAt) {
        return new NotificationResult(true, channelType, sentAt, null, null);
    }

    public static NotificationResult success(
            String channelType,
            Instant sentAt,
            String recipientSnapshot
    ) {
        return new NotificationResult(true, channelType, sentAt, null, recipientSnapshot);
    }

    public static NotificationResult failure(String channelType, String message) {
        return new NotificationResult(false, channelType, null, message, null);
    }

    public static NotificationResult failure(
            String channelType,
            String message,
            String recipientSnapshot
    ) {
        return new NotificationResult(false, channelType, null, message, recipientSnapshot);
    }
}

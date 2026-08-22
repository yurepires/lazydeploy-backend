package com.yurepires.lazydeploy.model.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationDeliveryAttempt(
        UUID id,
        UUID subscriptionId,
        UUID roundInstanceId,
        String channelType,
        String status,
        Instant attemptedAt,
        Instant sentAt,
        String errorCode,
        String errorMessage,
        Map<String, Object> metadata
) {
    public NotificationDeliveryAttempt {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

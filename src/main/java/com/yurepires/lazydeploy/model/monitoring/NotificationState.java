package com.yurepires.lazydeploy.model.monitoring;

import java.time.Instant;
import java.util.UUID;

public record NotificationState(
        UUID id,
        UUID subscriptionId,
        UUID roundInstanceId,
        NotificationStatus status,
        Instant lastAttemptAt,
        Instant notifiedAt
) {
    public static NotificationState pending(UUID subscriptionId, UUID roundInstanceId) {
        return new NotificationState(UUID.randomUUID(), subscriptionId, roundInstanceId, NotificationStatus.PENDING, null, null);
    }

    public String deduplicationKey() {
        return subscriptionId + ":" + roundInstanceId;
    }

    public NotificationState attemptedAt(Instant instant) {
        return new NotificationState(id, subscriptionId, roundInstanceId, status, instant, notifiedAt);
    }

    public NotificationState sentAt(Instant instant) {
        return new NotificationState(
                id, subscriptionId, roundInstanceId, NotificationStatus.SENT, instant, instant
        );
    }
}

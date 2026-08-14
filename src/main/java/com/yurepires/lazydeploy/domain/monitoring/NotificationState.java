package com.yurepires.lazydeploy.domain.monitoring;

import java.time.Instant;
import java.util.UUID;

public record NotificationState(
        String serverGuid,
        UUID roundInstanceId,
        NotificationStatus status,
        Instant lastAttemptAt,
        Instant notifiedAt
) {
    public static NotificationState pending(String serverGuid, UUID roundInstanceId) {
        return new NotificationState(serverGuid, roundInstanceId, NotificationStatus.PENDING, null, null);
    }

    public String deduplicationKey() {
        return serverGuid + ":" + roundInstanceId;
    }

    public NotificationState attemptedAt(Instant instant) {
        return new NotificationState(serverGuid, roundInstanceId, status, instant, notifiedAt);
    }

    public NotificationState sentAt(Instant instant) {
        return new NotificationState(
                serverGuid, roundInstanceId, NotificationStatus.SENT, instant, instant
        );
    }
}

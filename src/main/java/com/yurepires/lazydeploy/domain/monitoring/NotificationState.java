package com.yurepires.lazydeploy.domain.monitoring;

import java.time.Instant;

public record NotificationState(
        String serverId,
        String stateIdentity,
        NotificationStatus status,
        Instant lastAttemptAt,
        Instant notifiedAt
) {
    public static NotificationState pending(String serverId, String stateIdentity) {
        return new NotificationState(serverId, stateIdentity, NotificationStatus.PENDING, null, null);
    }

    public NotificationState attemptedAt(Instant instant) {
        return new NotificationState(serverId, stateIdentity, status, instant, notifiedAt);
    }

    public NotificationState sentAt(Instant instant) {
        return new NotificationState(
                serverId, stateIdentity, NotificationStatus.SENT, instant, instant
        );
    }
}

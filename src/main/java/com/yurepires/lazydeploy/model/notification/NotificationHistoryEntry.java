package com.yurepires.lazydeploy.model.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationHistoryEntry(
        UUID id,
        UUID subscriptionId,
        UUID serverId,
        String serverDisplayName,
        UUID roundInstanceId,
        String mapId,
        String mapDisplayName,
        Integer playerCount,
        Integer maxPlayers,
        String gameMode,
        String channelType,
        NotificationDeliveryStatus status,
        Instant attemptedAt,
        Instant sentAt,
        String errorCode,
        String errorMessage,
        String recipientSnapshot
) {
    public NotificationHistoryEntry(
            UUID id,
            UUID subscriptionId,
            UUID serverId,
            String serverDisplayName,
            UUID roundInstanceId,
            String mapId,
            String mapDisplayName,
            Integer playerCount,
            Integer maxPlayers,
            String gameMode,
            String channelType,
            NotificationDeliveryStatus status,
            Instant attemptedAt,
            Instant sentAt,
            String errorCode,
            String errorMessage
    ) {
        this(
                id,
                subscriptionId,
                serverId,
                serverDisplayName,
                roundInstanceId,
                mapId,
                mapDisplayName,
                playerCount,
                maxPlayers,
                gameMode,
                channelType,
                status,
                attemptedAt,
                sentAt,
                errorCode,
                errorMessage,
                null
        );
    }
}

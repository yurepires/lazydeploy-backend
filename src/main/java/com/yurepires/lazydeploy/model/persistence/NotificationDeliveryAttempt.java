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
        Map<String, Object> metadata,
        UUID ownerUserId,
        UUID serverId,
        String serverDisplayName,
        String mapId,
        String mapDisplayName,
        Integer playerCount,
        Integer maxPlayers,
        String gameMode,
        String recipientSnapshot
) {
    public NotificationDeliveryAttempt(
            UUID id,
            UUID subscriptionId,
            UUID ownerUserId,
            UUID roundInstanceId,
            UUID serverId,
            String serverDisplayName,
            String mapId,
            String mapDisplayName,
            Integer playerCount,
            Integer maxPlayers,
            String gameMode,
            String channelType,
            String status,
            Instant attemptedAt,
            Instant sentAt,
            String errorCode,
            String errorMessage,
            Map<String, Object> metadata
    ) {
        this(
                id,
                subscriptionId,
                roundInstanceId,
                channelType,
                status,
                attemptedAt,
                sentAt,
                errorCode,
                errorMessage,
                metadata,
                ownerUserId,
                serverId,
                serverDisplayName,
                mapId,
                mapDisplayName,
                playerCount,
                maxPlayers,
                gameMode,
                null
        );
    }

    public NotificationDeliveryAttempt(
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
        this(
                id,
                subscriptionId,
                roundInstanceId,
                channelType,
                status,
                attemptedAt,
                sentAt,
                errorCode,
                errorMessage,
                metadata,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public NotificationDeliveryAttempt {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;

import java.time.Instant;
import java.util.UUID;

public record NotificationHistoryResponse(
        UUID id,
        UUID subscriptionId,
        ServerInfo server,
        UUID roundInstanceId,
        MapInfo map,
        PlayersInfo players,
        String gameMode,
        String channelType,
        String status,
        Instant attemptedAt,
        Instant sentAt
) {
    public static NotificationHistoryResponse from(NotificationHistoryEntry entry) {
        return new NotificationHistoryResponse(
                entry.id(),
                entry.subscriptionId(),
                new ServerInfo(entry.serverId(), serverDisplayName(entry)),
                entry.roundInstanceId(),
                new MapInfo(entry.mapId(), mapDisplayName(entry)),
                new PlayersInfo(entry.playerCount(), entry.maxPlayers()),
                entry.gameMode(),
                entry.channelType(),
                entry.status() == null ? null : entry.status().name(),
                entry.attemptedAt(),
                entry.sentAt()
        );
    }

    private static String serverDisplayName(NotificationHistoryEntry entry) {
        if (entry.serverDisplayName() != null && !entry.serverDisplayName().isBlank()) {
            return entry.serverDisplayName();
        }
        if (entry.serverId() == null) {
            return null;
        }
        return entry.serverId().toString();
    }

    private static String mapDisplayName(NotificationHistoryEntry entry) {
        if (entry.mapDisplayName() != null && !entry.mapDisplayName().isBlank()) {
            return entry.mapDisplayName();
        }
        return entry.mapId();
    }

    public record ServerInfo(UUID id, String displayName) {
    }

    public record MapInfo(String id, String displayName) {
    }

    public record PlayersInfo(Integer current, Integer maximum) {
    }
}

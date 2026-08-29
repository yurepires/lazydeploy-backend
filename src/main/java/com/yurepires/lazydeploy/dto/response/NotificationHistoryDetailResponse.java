package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;

import java.time.Instant;
import java.util.UUID;

public record NotificationHistoryDetailResponse(
        UUID id,
        UUID subscriptionId,
        NotificationHistoryResponse.ServerInfo server,
        UUID roundInstanceId,
        NotificationHistoryResponse.MapInfo map,
        NotificationHistoryResponse.PlayersInfo players,
        String gameMode,
        String channelType,
        String status,
        Instant attemptedAt,
        Instant sentAt,
        String errorCode,
        String errorMessage
) {
    public static NotificationHistoryDetailResponse from(NotificationHistoryEntry entry) {
        NotificationHistoryResponse response = NotificationHistoryResponse.from(entry);
        return new NotificationHistoryDetailResponse(
                response.id(),
                response.subscriptionId(),
                response.server(),
                response.roundInstanceId(),
                response.map(),
                response.players(),
                response.gameMode(),
                response.channelType(),
                response.status(),
                response.attemptedAt(),
                response.sentAt(),
                entry.errorCode(),
                entry.errorMessage()
        );
    }
}

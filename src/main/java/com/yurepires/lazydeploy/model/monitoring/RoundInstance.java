package com.yurepires.lazydeploy.model.monitoring;

import java.time.Instant;
import java.util.UUID;

public record RoundInstance(
        UUID id,
        UUID serverId,
        String mapId,
        Instant detectedAt,
        long firstObservedRoundTime,
        Instant endedAt
) {
    public static RoundInstance detected(UUID serverId, String mapId, Instant detectedAt, long roundTime) {
        return new RoundInstance(UUID.randomUUID(), serverId, mapId, detectedAt, roundTime, null);
    }

    public RoundInstance endedAt(Instant instant) {
        return new RoundInstance(id, serverId, mapId, detectedAt, firstObservedRoundTime, instant);
    }
}

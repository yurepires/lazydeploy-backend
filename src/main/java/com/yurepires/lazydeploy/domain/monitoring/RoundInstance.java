package com.yurepires.lazydeploy.domain.monitoring;

import java.time.Instant;
import java.util.UUID;

public record RoundInstance(
        UUID id,
        String serverGuid,
        String mapId,
        Instant detectedAt,
        long firstObservedRoundTime
) {
    public static RoundInstance detected(String serverGuid, String mapId, Instant detectedAt, long roundTime) {
        return new RoundInstance(UUID.randomUUID(), serverGuid, mapId, detectedAt, roundTime);
    }
}

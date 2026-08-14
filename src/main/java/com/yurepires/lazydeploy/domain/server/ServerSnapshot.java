package com.yurepires.lazydeploy.domain.server;

import java.time.Instant;
import java.util.Map;

public record ServerSnapshot(
        String serverGuid,
        MapSnapshot map,
        PlayerSnapshot players,
        String gameMode,
        long roundTimeSeconds,
        Instant capturedAt,
        Map<String, Object> externalMetadata
) {
    public ServerSnapshot {
        externalMetadata = externalMetadata == null ? Map.of() : Map.copyOf(externalMetadata);
    }
}

package com.yurepires.lazydeploy.domain.server;

import java.time.Instant;

public record ServerSnapshot(
        String externalGuid,
        ServerAddress address,
        String name,
        MapSnapshot map,
        PlayerSnapshot players,
        String gameMode,
        RoundSnapshot round,
        Instant capturedAt
) {}

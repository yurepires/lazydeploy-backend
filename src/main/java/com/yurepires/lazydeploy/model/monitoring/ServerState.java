package com.yurepires.lazydeploy.model.monitoring;

import java.time.Instant;
import java.util.UUID;

public record ServerState(
        UUID serverId,
        UUID roundInstanceId,
        String mapId,
        long previousRoundTimeSeconds,
        Instant roundDetectedAt,
        Instant lastObservedAt,
        Integer playerCount,
        Integer maxPlayers,
        String gameMode
) {

    public ServerState(
            UUID serverId,
            UUID roundInstanceId,
            String mapId,
            long previousRoundTimeSeconds,
            Instant roundDetectedAt,
            Instant lastObservedAt
    ) {
        this(
                serverId,
                roundInstanceId,
                mapId,
                previousRoundTimeSeconds,
                roundDetectedAt,
                lastObservedAt,
                null,
                null,
                null
        );
    }
}

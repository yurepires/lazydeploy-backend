package com.yurepires.lazydeploy.domain.monitoring;

import java.time.Instant;
import java.util.UUID;

public record ServerState(
        String serverGuid,
        UUID roundInstanceId,
        String mapId,
        long previousRoundTimeSeconds,
        Instant roundDetectedAt,
        Instant lastObservedAt
) {}

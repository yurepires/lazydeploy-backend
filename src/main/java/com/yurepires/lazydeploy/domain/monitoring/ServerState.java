package com.yurepires.lazydeploy.domain.monitoring;

import com.yurepires.lazydeploy.domain.server.ServerSnapshot;

import java.time.Instant;

public record ServerState(
        String serverId,
        ServerSnapshot snapshot,
        String stateIdentity,
        Instant firstObservedAt,
        Instant lastObservedAt
) {}

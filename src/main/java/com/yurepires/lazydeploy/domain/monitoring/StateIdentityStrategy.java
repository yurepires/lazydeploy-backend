package com.yurepires.lazydeploy.domain.monitoring;

import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;

import java.time.Instant;

public interface StateIdentityStrategy {
    String createIdentity(
            MonitoredServer monitoredServer,
            ServerSnapshot currentSnapshot,
            ServerState previousState,
            Instant detectedAt
    );
}

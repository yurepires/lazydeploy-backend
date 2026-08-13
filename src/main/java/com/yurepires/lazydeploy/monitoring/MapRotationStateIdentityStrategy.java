package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.domain.monitoring.ServerState;
import com.yurepires.lazydeploy.domain.monitoring.StateIdentityStrategy;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class MapRotationStateIdentityStrategy implements StateIdentityStrategy {

    @Override
    public String createIdentity(MonitoredServer monitoredServer, ServerSnapshot currentSnapshot, ServerState previousState, Instant detectedAt) {
        if (previousState != null && Objects.equals(previousState.snapshot().map().id(), currentSnapshot.map().id())) {
            return previousState.stateIdentity();
        }

        return monitoredServer.id() + ":" + currentSnapshot.map().id() + ":" + detectedAt.toEpochMilli();
    }
}

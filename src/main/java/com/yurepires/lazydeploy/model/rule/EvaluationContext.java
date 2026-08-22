package com.yurepires.lazydeploy.model.rule;

import com.yurepires.lazydeploy.model.monitoring.NotificationState;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.server.MonitoredServer;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;

import java.time.Instant;

public record EvaluationContext(
        MonitoredServer configuration,
        ServerSnapshot currentSnapshot,
        ServerState previousState,
        NotificationState notificationState,
        Instant evaluatedAt
) {}

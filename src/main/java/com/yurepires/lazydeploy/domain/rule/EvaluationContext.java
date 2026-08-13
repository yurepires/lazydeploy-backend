package com.yurepires.lazydeploy.domain.rule;

import com.yurepires.lazydeploy.domain.monitoring.NotificationState;
import com.yurepires.lazydeploy.domain.monitoring.ServerState;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;

import java.time.Instant;

public record EvaluationContext(
        MonitoredServer configuration,
        ServerSnapshot currentSnapshot,
        ServerState previousState,
        NotificationState notificationState,
        Instant evaluatedAt
) {}

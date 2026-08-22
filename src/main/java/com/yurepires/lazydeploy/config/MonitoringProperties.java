package com.yurepires.lazydeploy.config;

import java.time.Duration;

public record MonitoringProperties(
        Duration interval,
        long roundResetThresholdSeconds,
        Duration maxObservationGap
) {}

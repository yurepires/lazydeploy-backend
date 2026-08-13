package com.yurepires.lazydeploy.domain.server;

public record RoundSnapshot(
        int played,
        int total,
        int elapsedSeconds
) {}

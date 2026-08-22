package com.yurepires.lazydeploy.model.server;

public record RoundSnapshot(
        int played,
        int total,
        int elapsedSeconds
) {}

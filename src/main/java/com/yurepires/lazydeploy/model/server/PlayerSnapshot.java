package com.yurepires.lazydeploy.model.server;

public record PlayerSnapshot(
        int current,
        int maximum,
        int waiting
) {}

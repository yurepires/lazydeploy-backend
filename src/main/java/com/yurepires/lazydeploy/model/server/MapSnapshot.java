package com.yurepires.lazydeploy.model.server;

public record MapSnapshot(
        String externalId,
        String normalizedId,
        String displayName
) {}

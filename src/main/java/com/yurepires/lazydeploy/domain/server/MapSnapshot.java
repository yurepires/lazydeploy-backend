package com.yurepires.lazydeploy.domain.server;

public record MapSnapshot(
        String externalId,
        String normalizedId,
        String displayName
) {}

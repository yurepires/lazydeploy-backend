package com.yurepires.lazydeploy.model.server;

import java.util.Map;
import java.util.UUID;

public record ServerReference(
        UUID internalId,
        String externalGuid,
        String displayName,
        String provider,
        Map<String, Object> metadata
) {
    public ServerReference {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

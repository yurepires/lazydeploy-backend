package com.yurepires.lazydeploy.model.persistence;

import java.util.UUID;

public record ServerIdentifier(
        UUID id,
        UUID serverId,
        String provider,
        String type,
        String value
) {}

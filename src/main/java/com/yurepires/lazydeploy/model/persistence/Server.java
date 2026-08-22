package com.yurepires.lazydeploy.model.persistence;

import java.time.Instant;
import java.util.UUID;

public record Server(
        UUID id,
        String displayName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}

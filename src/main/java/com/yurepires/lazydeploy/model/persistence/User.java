package com.yurepires.lazydeploy.model.persistence;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}

package com.yurepires.lazydeploy.model.notification;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record NotificationChannelConfiguration(
        UUID id,
        String type,
        boolean enabled,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant updatedAt
) {
    public NotificationChannelConfiguration(
            String type,
            boolean enabled,
            Map<String, Object> parameters
    ) {
        this(null, type, enabled, parameters, null, null);
    }

    public NotificationChannelConfiguration(
            UUID id,
            String type,
            boolean enabled,
            Map<String, Object> parameters
    ) {
        this(id, type, enabled, parameters, null, null);
    }

    public NotificationChannelConfiguration {
        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }
    }
}

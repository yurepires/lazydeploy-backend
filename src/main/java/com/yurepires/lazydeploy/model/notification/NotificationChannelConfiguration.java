package com.yurepires.lazydeploy.model.notification;

import java.util.Map;
import java.util.UUID;

public record NotificationChannelConfiguration(
        UUID id,
        String type,
        boolean enabled,
        Map<String, Object> parameters
) {
    public NotificationChannelConfiguration(String type, boolean enabled, Map<String, Object> parameters) {
        this(null, type, enabled, parameters);
    }

    public NotificationChannelConfiguration {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}

package com.yurepires.lazydeploy.domain.notification;

import java.util.Map;

public record NotificationChannelConfiguration(
        String type,
        boolean enabled,
        Map<String, Object> parameters
) {
    public NotificationChannelConfiguration {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}

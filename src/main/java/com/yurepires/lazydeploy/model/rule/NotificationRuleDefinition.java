package com.yurepires.lazydeploy.model.rule;

import java.util.Map;
import java.util.UUID;

public record NotificationRuleDefinition(
        UUID id,
        String type,
        boolean enabled,
        Map<String, Object> parameters
) {
    public NotificationRuleDefinition(String type, boolean enabled, Map<String, Object> parameters) {
        this(null, type, enabled, parameters);
    }

    public NotificationRuleDefinition {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}

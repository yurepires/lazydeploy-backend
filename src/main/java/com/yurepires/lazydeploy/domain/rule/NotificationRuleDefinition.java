package com.yurepires.lazydeploy.domain.rule;

import java.util.Map;

public record NotificationRuleDefinition(
        String type,
        boolean enabled,
        Map<String, Object> parameters
) {
    public NotificationRuleDefinition {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}

package com.yurepires.lazydeploy.model.rule;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record NotificationRuleDefinition(
        UUID id,
        String type,
        boolean enabled,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant updatedAt
) {
    public NotificationRuleDefinition(
            String type,
            boolean enabled,
            Map<String, Object> parameters
    ) {
        this(null, type, enabled, parameters, null, null);
    }

    public NotificationRuleDefinition(
            UUID id,
            String type,
            boolean enabled,
            Map<String, Object> parameters
    ) {
        this(id, type, enabled, parameters, null, null);
    }

    public NotificationRuleDefinition {
        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }
    }
}

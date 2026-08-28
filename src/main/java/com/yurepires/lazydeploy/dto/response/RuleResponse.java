package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        String type,
        boolean enabled,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant updatedAt
) {

    public RuleResponse {
        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Map.copyOf(parameters);
        }
    }

    public static RuleResponse from(NotificationRuleDefinition rule) {
        return new RuleResponse(
                rule.id(),
                rule.type(),
                rule.enabled(),
                rule.parameters(),
                rule.createdAt(),
                rule.updatedAt()
        );
    }
}

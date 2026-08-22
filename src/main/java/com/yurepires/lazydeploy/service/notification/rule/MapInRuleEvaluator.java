package com.yurepires.lazydeploy.service.notification.rule;

import com.yurepires.lazydeploy.model.rule.EvaluationContext;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.model.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.model.rule.RuleEvaluator;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class MapInRuleEvaluator implements RuleEvaluator {

    @Override
    public boolean supports(NotificationRuleDefinition rule) {
        return "MAP_IN".equalsIgnoreCase(rule.type());
    }

    @Override
    public RuleEvaluationResult evaluate(NotificationRuleDefinition rule, EvaluationContext context) {
        Object configured = rule.parameters().get("values");
        List<String> configuredMaps = parameterValues(configured);
        boolean matched = configuredMaps.stream().anyMatch(context.currentSnapshot().map().normalizedId()::equals);

        String reason = "MAP_NOT_MATCHED";
        if (matched) {
            reason = "MAP_MATCHED";
        }

        return new RuleEvaluationResult(
                matched,
                rule.type(),
                reason,
                Map.of(
                        "map", context.currentSnapshot().map().normalizedId(),
                        "configuredMaps", configuredMaps
                )
        );
    }

    private List<String> parameterValues(Object configured) {
        if (configured instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        if (configured instanceof Map<?, ?> indexedValues) {
            return indexedValues.values().stream().map(String::valueOf).toList();
        }
        if (configured == null) {
            return List.of();
        }

        return List.of(String.valueOf(configured));
    }
}

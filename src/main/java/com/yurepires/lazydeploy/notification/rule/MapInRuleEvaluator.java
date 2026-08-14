package com.yurepires.lazydeploy.notification.rule;

import com.yurepires.lazydeploy.domain.rule.EvaluationContext;
import com.yurepires.lazydeploy.domain.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluator;
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

        return new RuleEvaluationResult(
                matched,
                rule.type(),
                matched ? "MAP_MATCHED" : "MAP_NOT_MATCHED",
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
        return configured == null ? List.of() : List.of(String.valueOf(configured));
    }
}

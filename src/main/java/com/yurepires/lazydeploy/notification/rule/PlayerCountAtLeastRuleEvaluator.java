package com.yurepires.lazydeploy.notification.rule;

import com.yurepires.lazydeploy.domain.rule.EvaluationContext;
import com.yurepires.lazydeploy.domain.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlayerCountAtLeastRuleEvaluator implements RuleEvaluator {

    @Override
    public boolean supports(NotificationRuleDefinition rule) {
        return "PLAYER_COUNT_AT_LEAST".equalsIgnoreCase(rule.type());
    }

    @Override
    public RuleEvaluationResult evaluate(NotificationRuleDefinition rule, EvaluationContext context) {
        Object configured = rule.parameters().get("value");
        if (!(configured instanceof Number number)) {
            throw new IllegalArgumentException("PLAYER_COUNT_AT_LEAST requer parâmetro numérico 'value'");
        }

        int minimum = number.intValue();
        if (minimum < 0) {
            throw new IllegalArgumentException("PLAYER_COUNT_AT_LEAST não aceita valor negativo");
        }

        int current = context.currentSnapshot().players().current();
        boolean matched = current >= minimum;
        return new RuleEvaluationResult(
                matched,
                rule.type(),
                matched ? "MINIMUM_REACHED" : "MINIMUM_NOT_REACHED",
                Map.of("current", current, "minimum", minimum)
        );
    }
}

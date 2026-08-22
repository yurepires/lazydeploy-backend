package com.yurepires.lazydeploy.service.notification.rule;

import com.yurepires.lazydeploy.exception.InvalidRequestException;
import com.yurepires.lazydeploy.model.rule.EvaluationContext;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.model.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.model.rule.RuleEvaluator;
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
            throw new InvalidRequestException(
                    "PLAYER_COUNT_AT_LEAST requer parâmetro numérico 'value'"
            );
        }

        int minimum = number.intValue();
        if (minimum < 0) {
            throw new InvalidRequestException(
                    "PLAYER_COUNT_AT_LEAST não aceita valor negativo"
            );
        }

        int current = context.currentSnapshot().players().current();
        boolean matched = current >= minimum;
        String reason = "MINIMUM_NOT_REACHED";
        if (matched) {
            reason = "MINIMUM_REACHED";
        }

        return new RuleEvaluationResult(
                matched,
                rule.type(),
                reason,
                Map.of("current", current, "minimum", minimum)
        );
    }
}

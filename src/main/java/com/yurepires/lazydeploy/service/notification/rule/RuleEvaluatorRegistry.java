package com.yurepires.lazydeploy.service.notification.rule;

import com.yurepires.lazydeploy.exception.UnsupportedRuleTypeException;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.model.rule.RuleEvaluator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEvaluatorRegistry {

    private final List<RuleEvaluator> evaluators;

    public RuleEvaluatorRegistry(List<RuleEvaluator> evaluators) {
        this.evaluators = List.copyOf(evaluators);
    }

    public RuleEvaluator resolve(NotificationRuleDefinition rule) {
        return evaluators.stream()
                .filter(evaluator -> evaluator.supports(rule))
                .findFirst()
                .orElseThrow(() -> new UnsupportedRuleTypeException(rule.type()));
    }
}

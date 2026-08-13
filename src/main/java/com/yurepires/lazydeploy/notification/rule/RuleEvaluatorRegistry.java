package com.yurepires.lazydeploy.notification.rule;

import com.yurepires.lazydeploy.domain.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluator;
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
                .orElseThrow(() -> new IllegalArgumentException("Nenhum RuleEvaluator registrado para a regra " + rule.type()));
    }
}

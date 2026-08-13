package com.yurepires.lazydeploy.domain.rule;

public interface RuleEvaluator {
    boolean supports(NotificationRuleDefinition rule);

    RuleEvaluationResult evaluate(NotificationRuleDefinition rule, EvaluationContext context);
}

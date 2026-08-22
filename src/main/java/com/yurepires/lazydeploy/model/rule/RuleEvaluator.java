package com.yurepires.lazydeploy.model.rule;

public interface RuleEvaluator {
    boolean supports(NotificationRuleDefinition rule);

    RuleEvaluationResult evaluate(NotificationRuleDefinition rule, EvaluationContext context);
}

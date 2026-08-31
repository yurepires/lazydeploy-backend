package com.yurepires.lazydeploy.service.notification.rule;

import com.yurepires.lazydeploy.model.monitoring.NotificationStatus;
import com.yurepires.lazydeploy.model.notification.NotificationDecision;
import com.yurepires.lazydeploy.model.rule.EvaluationContext;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.model.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.service.observability.NotificationMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationEvaluationService {

    private final RuleEvaluatorRegistry registry;
    private final NotificationMetrics metrics;

    public NotificationEvaluationService(RuleEvaluatorRegistry registry) {
        this(registry, NotificationMetrics.noop());
    }

    @Autowired
    public NotificationEvaluationService(
            RuleEvaluatorRegistry registry,
            NotificationMetrics metrics
    ) {
        this.registry = registry;
        this.metrics = metrics;
    }

    public NotificationDecision evaluate(EvaluationContext context) {
        if (!context.configuration().enabled()) {
            return rejected(context, "SERVER", "SERVER_DISABLED");
        }
        if (context.notificationState().status() == NotificationStatus.SENT) {
            return rejected(context, "NOTIFICATION_STATE", "ALREADY_SENT");
        }

        List<RuleEvaluationResult> results = new ArrayList<>();
        for (NotificationRuleDefinition rule : context.configuration().rules()) {
            if (!rule.enabled()) {
                continue;
            }
            try {
                RuleEvaluationResult result = registry.resolve(rule).evaluate(rule, context);
                metrics.recordRuleEvaluation(
                        result.ruleType(),
                        evaluationResult(result)
                );
                results.add(result);
            } catch (RuntimeException exception) {
                metrics.recordRuleEvaluation(rule.type(), "invalid");
                throw exception;
            }
        }

        boolean approved = results.stream().allMatch(RuleEvaluationResult::matched);
        return new NotificationDecision(approved, results, context.evaluatedAt());
    }

    private String evaluationResult(RuleEvaluationResult result) {
        if (result.matched()) {
            return "matched";
        }
        return "not_matched";
    }

    private NotificationDecision rejected(EvaluationContext context, String type, String reason) {
        metrics.recordRuleEvaluation(type, "not_matched");
        return new NotificationDecision(false, List.of(new RuleEvaluationResult(false, type, reason, Map.of())), context.evaluatedAt());
    }
}

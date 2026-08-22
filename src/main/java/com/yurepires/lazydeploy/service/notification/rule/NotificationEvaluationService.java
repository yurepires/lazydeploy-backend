package com.yurepires.lazydeploy.service.notification.rule;

import com.yurepires.lazydeploy.model.monitoring.NotificationStatus;
import com.yurepires.lazydeploy.model.notification.NotificationDecision;
import com.yurepires.lazydeploy.model.rule.EvaluationContext;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.model.rule.RuleEvaluationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationEvaluationService {

    private final RuleEvaluatorRegistry registry;

    public NotificationEvaluationService(RuleEvaluatorRegistry registry) {
        this.registry = registry;
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
            results.add(registry.resolve(rule).evaluate(rule, context));
        }

        boolean approved = results.stream().allMatch(RuleEvaluationResult::matched);
        return new NotificationDecision(approved, results, context.evaluatedAt());
    }

    private NotificationDecision rejected(EvaluationContext context, String type, String reason) {
        return new NotificationDecision(false, List.of(new RuleEvaluationResult(false, type, reason, Map.of())), context.evaluatedAt());
    }
}

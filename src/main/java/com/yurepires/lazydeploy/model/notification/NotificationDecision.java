package com.yurepires.lazydeploy.model.notification;

import com.yurepires.lazydeploy.model.rule.RuleEvaluationResult;

import java.time.Instant;
import java.util.List;

public record NotificationDecision(
        boolean approved,
        List<RuleEvaluationResult> results,
        Instant evaluatedAt
) {
    public NotificationDecision {
        results = List.copyOf(results);
    }
}

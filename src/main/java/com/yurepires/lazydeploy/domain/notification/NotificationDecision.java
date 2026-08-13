package com.yurepires.lazydeploy.domain.notification;

import com.yurepires.lazydeploy.domain.rule.RuleEvaluationResult;

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

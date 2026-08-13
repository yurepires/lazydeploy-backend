package com.yurepires.lazydeploy.domain.rule;

import java.util.Map;

public record RuleEvaluationResult(
        boolean matched,
        String ruleType,
        String reason,
        Map<String, Object> metadata
) {
    public RuleEvaluationResult {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

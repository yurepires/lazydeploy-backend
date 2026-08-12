package com.yurepires.lazydeploy.notification.rule;

public record NotificationDecision(
        boolean shouldNotify,
        NotificationDecisionReason reason
) {}

package com.yurepires.lazydeploy.exception;

public class NotificationRuleNotFoundException extends ResourceNotFoundException {

    public NotificationRuleNotFoundException() {
        super("NOTIFICATION_RULE_NOT_FOUND", "Regra de notificação não encontrada");
    }
}

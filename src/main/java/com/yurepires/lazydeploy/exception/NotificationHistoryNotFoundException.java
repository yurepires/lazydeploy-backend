package com.yurepires.lazydeploy.exception;

public class NotificationHistoryNotFoundException extends ResourceNotFoundException {

    public NotificationHistoryNotFoundException() {
        super("RESOURCE_NOT_FOUND", "Histórico de notificação não encontrado");
    }
}

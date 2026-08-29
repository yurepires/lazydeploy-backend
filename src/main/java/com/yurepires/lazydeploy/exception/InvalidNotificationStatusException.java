package com.yurepires.lazydeploy.exception;

public class InvalidNotificationStatusException extends InvalidRequestException {

    public InvalidNotificationStatusException(String status) {
        super(
                "INVALID_NOTIFICATION_STATUS",
                "Status de notificação inválido: " + status
        );
    }
}

package com.yurepires.lazydeploy.exception;

public class NotificationRecipientUnavailableException extends ApplicationException {

    public NotificationRecipientUnavailableException() {
        super(
                "NOTIFICATION_RECIPIENT_UNAVAILABLE",
                "O endereço de e-mail da conta não está disponível para notificação"
        );
    }
}

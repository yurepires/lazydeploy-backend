package com.yurepires.lazydeploy.exception;

public class NotificationChannelNotFoundException extends ResourceNotFoundException {

    public NotificationChannelNotFoundException() {
        super("NOTIFICATION_CHANNEL_NOT_FOUND", "Canal de notificação não encontrado");
    }
}

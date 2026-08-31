package com.yurepires.lazydeploy.exception;

public class NoActiveNotificationChannelException extends ApplicationException {

    public NoActiveNotificationChannelException() {
        super(
                "NO_ACTIVE_NOTIFICATION_CHANNEL",
                "Uma subscription habilitada deve possuir pelo menos um canal ativo"
        );
    }
}

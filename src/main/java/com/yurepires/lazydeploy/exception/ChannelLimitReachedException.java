package com.yurepires.lazydeploy.exception;

public class ChannelLimitReachedException extends ApplicationException {

    public ChannelLimitReachedException() {
        super(
                "CHANNEL_LIMIT_REACHED",
                "Este alerta atingiu o número máximo de canais de notificação permitidos."
        );
    }
}

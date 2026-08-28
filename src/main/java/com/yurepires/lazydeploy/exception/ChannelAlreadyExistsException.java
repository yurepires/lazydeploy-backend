package com.yurepires.lazydeploy.exception;

public class ChannelAlreadyExistsException extends ApplicationException {

    public ChannelAlreadyExistsException() {
        super("CHANNEL_ALREADY_EXISTS", "Já existe um canal deste tipo para esta subscription");
    }
}

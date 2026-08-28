package com.yurepires.lazydeploy.exception;

public class UnsupportedChannelTypeException extends ApplicationException {

    public UnsupportedChannelTypeException(String type) {
        super("UNSUPPORTED_CHANNEL_TYPE", "Tipo de canal não suportado: " + type);
    }
}

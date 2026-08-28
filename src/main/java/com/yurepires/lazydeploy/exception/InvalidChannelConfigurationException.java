package com.yurepires.lazydeploy.exception;

public class InvalidChannelConfigurationException extends ApplicationException {

    public InvalidChannelConfigurationException(String message) {
        super("INVALID_CHANNEL_CONFIGURATION", message);
    }
}

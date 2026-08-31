package com.yurepires.lazydeploy.exception;

public class DuplicateChannelTypeException extends ApplicationException {

    public DuplicateChannelTypeException(String type) {
        super(
                "DUPLICATE_CHANNEL_TYPE",
                "O canal " + type + " foi informado mais de uma vez"
        );
    }
}

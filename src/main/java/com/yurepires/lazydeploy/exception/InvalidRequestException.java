package com.yurepires.lazydeploy.exception;

public class InvalidRequestException extends ApplicationException {

    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }

    protected InvalidRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}

package com.yurepires.lazydeploy.exception;

public class InvalidPageException extends InvalidRequestException {

    public InvalidPageException(String message) {
        super("INVALID_PAGE", message);
    }
}

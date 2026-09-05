package com.yurepires.lazydeploy.exception;

public class InvalidPageSizeException extends InvalidRequestException {

    public InvalidPageSizeException(String message) {
        super("INVALID_PAGE_SIZE", message);
    }
}

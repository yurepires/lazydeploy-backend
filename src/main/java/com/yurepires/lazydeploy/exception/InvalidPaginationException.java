package com.yurepires.lazydeploy.exception;

public class InvalidPaginationException extends InvalidRequestException {

    public InvalidPaginationException(String message) {
        super("INVALID_PAGINATION", message);
    }
}

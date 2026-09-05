package com.yurepires.lazydeploy.exception;

public class PageSizeLimitExceededException extends InvalidRequestException {

    public PageSizeLimitExceededException(String message) {
        super("PAGE_SIZE_LIMIT_EXCEEDED", message);
    }
}

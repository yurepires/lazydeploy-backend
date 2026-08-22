package com.yurepires.lazydeploy.exception;

public abstract class ResourceNotFoundException extends ApplicationException {

    protected ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}

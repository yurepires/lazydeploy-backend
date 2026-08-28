package com.yurepires.lazydeploy.exception;

public class ExternalProviderUnavailableException extends ApplicationException {

    public ExternalProviderUnavailableException(String message) {
        super("EXTERNAL_PROVIDER_UNAVAILABLE", message);
    }
}

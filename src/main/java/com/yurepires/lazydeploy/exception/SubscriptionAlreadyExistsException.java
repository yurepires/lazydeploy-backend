package com.yurepires.lazydeploy.exception;

public class SubscriptionAlreadyExistsException extends ApplicationException {

    public SubscriptionAlreadyExistsException() {
        super("SUBSCRIPTION_ALREADY_EXISTS", "Você já acompanha este servidor");
    }
}

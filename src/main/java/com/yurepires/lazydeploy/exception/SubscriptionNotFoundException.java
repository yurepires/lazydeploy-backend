package com.yurepires.lazydeploy.exception;

public class SubscriptionNotFoundException extends ResourceNotFoundException {

    public SubscriptionNotFoundException() {
        super("SUBSCRIPTION_NOT_FOUND", "Inscrição não encontrada");
    }
}

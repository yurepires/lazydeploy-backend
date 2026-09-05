package com.yurepires.lazydeploy.exception;

public class SubscriptionLimitReachedException extends ApplicationException {

    public SubscriptionLimitReachedException() {
        super(
                "SUBSCRIPTION_LIMIT_REACHED",
                "Você atingiu o número máximo de alertas permitidos."
        );
    }
}

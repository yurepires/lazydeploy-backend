package com.yurepires.lazydeploy.service.observability;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("mail")
public class MailHealthIndicator implements HealthIndicator {

    public static final String PROVIDER_ID = "MAILJET";

    private final ExternalProviderHealthTracker healthTracker;

    public MailHealthIndicator(ExternalProviderHealthTracker healthTracker) {
        this.healthTracker = healthTracker;
    }

    @Override
    public Health health() {
        return ProviderHealthIndicatorSupport.createHealth(
                healthTracker.getStatus(PROVIDER_ID)
        );
    }
}

package com.yurepires.lazydeploy.service.observability;

import com.yurepires.lazydeploy.integration.gametools.GameToolsServerDiscoveryProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("gameTools")
public class GameToolsHealthIndicator implements HealthIndicator {

    private final ExternalProviderHealthTracker healthTracker;

    public GameToolsHealthIndicator(ExternalProviderHealthTracker healthTracker) {
        this.healthTracker = healthTracker;
    }

    @Override
    public Health health() {
        return ProviderHealthIndicatorSupport.createHealth(
                healthTracker.getStatus(GameToolsServerDiscoveryProvider.PROVIDER_ID)
        );
    }
}

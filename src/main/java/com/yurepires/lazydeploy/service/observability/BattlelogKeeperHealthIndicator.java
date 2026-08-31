package com.yurepires.lazydeploy.service.observability;

import com.yurepires.lazydeploy.integration.battlelogkeeper.BattlelogKeeperSnapshotProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("battlelogKeeper")
public class BattlelogKeeperHealthIndicator implements HealthIndicator {

    private final ExternalProviderHealthTracker healthTracker;

    public BattlelogKeeperHealthIndicator(ExternalProviderHealthTracker healthTracker) {
        this.healthTracker = healthTracker;
    }

    @Override
    public Health health() {
        return ProviderHealthIndicatorSupport.createHealth(
                healthTracker.getStatus(BattlelogKeeperSnapshotProvider.PROVIDER_ID)
        );
    }
}

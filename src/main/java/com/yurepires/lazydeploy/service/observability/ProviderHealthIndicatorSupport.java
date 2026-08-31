package com.yurepires.lazydeploy.service.observability;

import org.springframework.boot.health.contributor.Health;

/**
 * Constrói respostas uniformes para health indicators baseados no tracker.
 */
public final class ProviderHealthIndicatorSupport {

    private ProviderHealthIndicatorSupport() {
    }

    public static Health createHealth(ProviderHealthStatus status) {
        Health.Builder builder;
        if (!status.hasAttempt()) {
            builder = Health.unknown();
        } else if (status.consecutiveFailures() > 0) {
            builder = Health.down();
        } else {
            builder = Health.up();
        }

        builder.withDetail("provider", status.providerId());
        builder.withDetail("consecutiveFailures", status.consecutiveFailures());

        if (status.lastSuccessAt() != null) {
            builder.withDetail("lastSuccessAt", status.lastSuccessAt());
        }
        if (status.lastFailureAt() != null) {
            builder.withDetail("lastFailureAt", status.lastFailureAt());
        }
        if (status.lastFailureCategory() != null) {
            builder.withDetail("lastFailureCategory", status.lastFailureCategory());
        }

        return builder.build();
    }
}

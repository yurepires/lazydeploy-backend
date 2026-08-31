package com.yurepires.lazydeploy.service.observability;

import java.time.Instant;

/**
 * Visão imutável do estado operacional mais recente de um provider.
 */
public final class ProviderHealthStatus {

    private final String providerId;
    private final Instant lastSuccessAt;
    private final Instant lastFailureAt;
    private final long consecutiveFailures;
    private final String lastFailureCategory;

    public ProviderHealthStatus(
            String providerId,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            long consecutiveFailures,
            String lastFailureCategory
    ) {
        this.providerId = providerId;
        this.lastSuccessAt = lastSuccessAt;
        this.lastFailureAt = lastFailureAt;
        this.consecutiveFailures = consecutiveFailures;
        this.lastFailureCategory = lastFailureCategory;
    }

    public String providerId() {
        return providerId;
    }

    public Instant lastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant lastFailureAt() {
        return lastFailureAt;
    }

    public long consecutiveFailures() {
        return consecutiveFailures;
    }

    public String lastFailureCategory() {
        return lastFailureCategory;
    }

    public boolean hasAttempt() {
        return lastSuccessAt != null || lastFailureAt != null;
    }
}

package com.yurepires.lazydeploy.service.observability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Mantém em memória o resultado da última interação com providers externos.
 *
 * <p>O tracker não faz chamadas externas e não armazena exceções. Ele serve
 * apenas como fonte local para health indicators e diagnóstico operacional.</p>
 */
@Component
public class ExternalProviderHealthTracker {

    private final Clock clock;
    private final ConcurrentMap<String, ProviderState> states = new ConcurrentHashMap<>();

    public ExternalProviderHealthTracker() {
        this(Clock.systemUTC());
    }

    @Autowired
    public ExternalProviderHealthTracker(Clock clock) {
        this.clock = clock;
    }

    public void recordSuccess(String providerId) {
        String normalizedProviderId = normalizeProviderId(providerId);
        Instant now = Instant.now(clock);
        states.compute(normalizedProviderId, (key, current) -> {
            ProviderState state = stateFor(current);
            synchronized (state) {
                state.lastSuccessAt = now;
                state.consecutiveFailures = 0;
                state.lastFailureCategory = null;
            }
            return state;
        });
    }

    public void recordFailure(String providerId, String failureCategory) {
        String normalizedProviderId = normalizeProviderId(providerId);
        Instant now = Instant.now(clock);
        states.compute(normalizedProviderId, (key, current) -> {
            ProviderState state = stateFor(current);
            synchronized (state) {
                state.lastFailureAt = now;
                state.consecutiveFailures++;
                state.lastFailureCategory = normalizeCategory(failureCategory);
            }
            return state;
        });
    }

    public ProviderHealthStatus getStatus(String providerId) {
        String normalizedProviderId = normalizeProviderId(providerId);
        ProviderState state = states.get(normalizedProviderId);
        if (state == null) {
            return new ProviderHealthStatus(normalizedProviderId, null, null, 0, null);
        }

        synchronized (state) {
            return new ProviderHealthStatus(
                    normalizedProviderId,
                    state.lastSuccessAt,
                    state.lastFailureAt,
                    state.consecutiveFailures,
                    state.lastFailureCategory
            );
        }
    }

    private String normalizeProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return "UNKNOWN";
        }
        return providerId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "OTHER";
        }
        return category.trim().toUpperCase(Locale.ROOT);
    }

    private ProviderState stateFor(ProviderState current) {
        if (current == null) {
            return new ProviderState();
        }
        return current;
    }

    private static final class ProviderState {
        private Instant lastSuccessAt;
        private Instant lastFailureAt;
        private long consecutiveFailures;
        private String lastFailureCategory;
    }
}

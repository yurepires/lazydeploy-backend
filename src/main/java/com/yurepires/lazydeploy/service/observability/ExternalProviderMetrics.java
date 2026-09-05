package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/** Métricas agregadas, com tags limitadas aos providers e resultados conhecidos. */
@Component
public class ExternalProviderMetrics {

    public static final String REQUESTS = "lazydeploy.provider.requests";
    public static final String DURATION = "lazydeploy.provider.duration";
    public static final String TIMEOUTS = "lazydeploy.provider.timeouts";
    public static final String CONCURRENCY_REJECTIONS =
            "lazydeploy.provider.concurrency_rejections";
    public static final String RETRIES = "lazydeploy.provider.retries";

    private static final Set<String> PROVIDERS = Set.of(
            "gametools",
            "keeper",
            "bflist"
    );
    private static final Set<String> OUTCOMES = Set.of(
            "success",
            "timeout",
            "connection_error",
            "client_error",
            "server_error",
            "invalid_response",
            "rate_limited",
            "concurrency_rejected",
            "other_error"
    );

    private final MeterRegistry meterRegistry;

    public ExternalProviderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static ExternalProviderMetrics noop() {
        return new ExternalProviderMetrics(null);
    }

    public void recordRequest(String provider, String outcome, Duration duration) {
        if (meterRegistry == null) {
            return;
        }

        String normalizedProvider = normalizeProvider(provider);
        String normalizedOutcome = normalizeOutcome(outcome);
        try {
            Counter.builder(REQUESTS)
                    .tag("provider", normalizedProvider)
                    .tag("outcome", normalizedOutcome)
                    .description("Chamadas realizadas a providers externos")
                    .register(meterRegistry)
                    .increment();
            Timer.builder(DURATION)
                    .tag("provider", normalizedProvider)
                    .tag("outcome", normalizedOutcome)
                    .description("Duração das chamadas a providers externos")
                    .register(meterRegistry)
                    .record(nonNegative(duration));
        } catch (RuntimeException ignored) {
            // Métricas não podem interromper a chamada externa.
        }
    }

    public void recordTimeout(String provider) {
        if (meterRegistry == null) {
            return;
        }
        incrementSingleTag(TIMEOUTS, "provider", normalizeProvider(provider));
    }

    public void recordConcurrencyRejection(String provider) {
        if (meterRegistry == null) {
            return;
        }
        incrementSingleTag(
                CONCURRENCY_REJECTIONS,
                "provider",
                normalizeProvider(provider)
        );
    }

    public void recordRetry(String provider, String outcome) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(RETRIES)
                    .tag("provider", normalizeProvider(provider))
                    .tag("outcome", normalizeOutcome(outcome))
                    .description("Retries executados para providers externos")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Métricas não podem interromper a política de retry.
        }
    }

    private void incrementSingleTag(String metricName, String tagName, String tagValue) {
        try {
            Counter.builder(metricName)
                    .tag(tagName, tagValue)
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Métricas não podem interromper a integração.
        }
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return "bflist";
        }

        String normalizedProvider = provider.trim().toLowerCase(Locale.ROOT);
        if (normalizedProvider.equals("battlelog_keeper")
                || normalizedProvider.equals("battlelog-keeper")) {
            return "keeper";
        }
        if (normalizedProvider.equals("game_tools")
                || normalizedProvider.equals("game-tools")) {
            return "gametools";
        }
        if (PROVIDERS.contains(normalizedProvider)) {
            return normalizedProvider;
        }
        return "bflist";
    }

    private String normalizeOutcome(String outcome) {
        if (outcome == null) {
            return "other_error";
        }

        String normalizedOutcome = outcome.trim().toLowerCase(Locale.ROOT);
        if (OUTCOMES.contains(normalizedOutcome)) {
            return normalizedOutcome;
        }
        return "other_error";
    }

    private Duration nonNegative(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }
}

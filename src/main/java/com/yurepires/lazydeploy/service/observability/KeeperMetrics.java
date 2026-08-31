package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Métricas da integração com o Battlelog Keeper.
 */
@Component
public class KeeperMetrics {

    private static final String REQUEST_COUNTER = "lazydeploy.keeper.requests";
    private static final String REQUEST_DURATION = "lazydeploy.keeper.request.duration";
    private static final Set<String> OUTCOMES = Set.of(
            "success",
            "timeout",
            "client_error",
            "server_error",
            "invalid_snapshot",
            "other_error"
    );

    private final MeterRegistry meterRegistry;

    public KeeperMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static KeeperMetrics noop() {
        return new KeeperMetrics(null);
    }

    public void recordRequest(String outcome, Duration duration) {
        if (meterRegistry == null) {
            return;
        }

        try {
            String normalizedOutcome = normalizeOutcome(outcome);
            Counter.builder(REQUEST_COUNTER)
                    .tag("outcome", normalizedOutcome)
                    .description("Requisições feitas ao Battlelog Keeper")
                    .register(meterRegistry)
                    .increment();
            Timer.builder(REQUEST_DURATION)
                    .tag("outcome", normalizedOutcome)
                    .description("Duração das requisições ao Battlelog Keeper")
                    .register(meterRegistry)
                    .record(nonNegative(duration));
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper a integração.
        }
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

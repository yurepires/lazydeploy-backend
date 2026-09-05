package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Métricas da busca opcional de servidores no GameTools.
 */
@Component
public class GameToolsMetrics {

    private static final String REQUEST_COUNTER = "lazydeploy.gametools.search.requests";
    private static final String REQUEST_DURATION = "lazydeploy.gametools.search.duration";
    private static final String RESULT_COUNT = "lazydeploy.gametools.search.results";
    private static final Set<String> OUTCOMES = Set.of(
            "success",
            "timeout",
            "client_error",
            "server_error",
            "invalid_response",
            "other_error"
    );

    private final MeterRegistry meterRegistry;

    public GameToolsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static GameToolsMetrics noop() {
        return new GameToolsMetrics(null);
    }

    public void recordRequest(String outcome, Duration duration) {
        if (meterRegistry == null) {
            return;
        }

        try {
            String normalizedOutcome = normalizeOutcome(outcome);
            Counter.builder(REQUEST_COUNTER)
                    .tag("outcome", normalizedOutcome)
                    .description("Buscas feitas no GameTools")
                    .register(meterRegistry)
                    .increment();
            Timer.builder(REQUEST_DURATION)
                    .tag("outcome", normalizedOutcome)
                    .description("Duração das buscas no GameTools")
                    .register(meterRegistry)
                    .record(nonNegative(duration));
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper a integração.
        }
    }

    public void recordResultCount(int resultCount) {
        if (meterRegistry == null) {
            return;
        }

        try {
            DistributionSummary.builder(RESULT_COUNT)
                    .description("Quantidade de resultados retornados pelo GameTools")
                    .register(meterRegistry)
                    .record(Math.max(resultCount, 0));
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

package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Métricas da configuração atômica de subscriptions.
 *
 * <p>Nenhum identificador de usuário, servidor ou subscription é usado como
 * tag. Isso mantém a cardinalidade controlada mesmo com muitos usuários.</p>
 */
@Component
public class SubscriptionConfigurationMetrics {

    private static final String CONFIGURE_ATTEMPTS =
            "lazydeploy.subscription.configure.attempts";
    private static final String CONFIGURE_DURATION =
            "lazydeploy.subscription.configure.duration";

    private static final Set<String> OUTCOMES = Set.of(
            "success",
            "validation_error",
            "duplicate",
            "provider_error",
            "internal_error"
    );

    private final MeterRegistry meterRegistry;

    public SubscriptionConfigurationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static SubscriptionConfigurationMetrics noop() {
        return new SubscriptionConfigurationMetrics(null);
    }

    public void record(String outcome, Duration duration) {
        if (meterRegistry == null) {
            return;
        }

        try {
            String normalizedOutcome = normalizeOutcome(outcome);
            Counter.builder(CONFIGURE_ATTEMPTS)
                    .tag("outcome", normalizedOutcome)
                    .description("Tentativas de configuração atômica de subscriptions")
                    .register(meterRegistry)
                    .increment();
            Timer.builder(CONFIGURE_DURATION)
                    .tag("outcome", normalizedOutcome)
                    .description("Duração da configuração atômica de subscriptions")
                    .register(meterRegistry)
                    .record(nonNegative(duration));
        } catch (RuntimeException ignored) {
            // Métricas não podem interromper a configuração da subscription.
        }
    }

    private String normalizeOutcome(String outcome) {
        if (outcome == null) {
            return "internal_error";
        }

        String normalizedOutcome = outcome.trim().toLowerCase(Locale.ROOT);
        if (OUTCOMES.contains(normalizedOutcome)) {
            return normalizedOutcome;
        }

        return "internal_error";
    }

    private Duration nonNegative(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }

        return duration;
    }
}

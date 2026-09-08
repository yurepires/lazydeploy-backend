package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas agregadas da avaliação e da entrega de notificações.
 */
@Component
public class NotificationMetrics {

    private static final String RULE_EVALUATIONS = "lazydeploy.rules.evaluations";
    private static final String CANDIDATES = "lazydeploy.notifications.candidates";
    private static final String DELIVERY_ATTEMPTS = "lazydeploy.notification.delivery.attempts";
    private static final String DELIVERY_DURATION = "lazydeploy.notification.delivery.duration";

    private static final Set<String> RULE_TYPES = Set.of("MAP_IN", "PLAYER_COUNT_AT_LEAST");
    private static final Set<String> RULE_RESULTS = Set.of("matched", "not_matched", "invalid");
    private static final Set<String> CANDIDATE_RESULTS = Set.of(
            "approved",
            "already_sent",
            "no_active_channel",
            "rejected"
    );
    private static final Set<String> DELIVERY_OUTCOMES = Set.of("success", "failed");

    private final MeterRegistry meterRegistry;
    private final AtomicLong successfulDeliveries = new AtomicLong();

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static NotificationMetrics noop() {
        return new NotificationMetrics(null);
    }

    public void recordRuleEvaluation(String ruleType, String result) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(RULE_EVALUATIONS)
                    .tag("type", normalizeRuleType(ruleType))
                    .tag("result", normalize(result, RULE_RESULTS, "invalid"))
                    .description("Avaliações de regras de notificação")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper as notificações.
        }
    }

    public void recordCandidate(String result) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(CANDIDATES)
                    .tag("result", normalize(result, CANDIDATE_RESULTS, "rejected"))
                    .description("Candidates de notificação avaliados")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper as notificações.
        }
    }

    public void recordDelivery(String channel, String outcome, Duration duration) {
        if ("success".equalsIgnoreCase(outcome)) {
            successfulDeliveries.incrementAndGet();
        }
        if (meterRegistry == null) {
            return;
        }

        try {
            String normalizedChannel = normalizeChannel(channel);
            String normalizedOutcome = normalize(outcome, DELIVERY_OUTCOMES, "failed");
            Counter.builder(DELIVERY_ATTEMPTS)
                    .tag("channel", normalizedChannel)
                    .tag("outcome", normalizedOutcome)
                    .description("Tentativas de entrega de notificações")
                    .register(meterRegistry)
                    .increment();
            Timer.builder(DELIVERY_DURATION)
                    .tag("channel", normalizedChannel)
                    .tag("outcome", normalizedOutcome)
                    .description("Duração da entrega de notificações")
                    .register(meterRegistry)
                    .record(nonNegative(duration));
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper as notificações.
        }
    }

    public long successfulDeliveryCount() {
        return successfulDeliveries.get();
    }

    private String normalizeRuleType(String ruleType) {
        if (ruleType == null) {
            return "other";
        }

        String normalizedRuleType = ruleType.trim().toUpperCase(Locale.ROOT);
        if (RULE_TYPES.contains(normalizedRuleType)) {
            return normalizedRuleType;
        }
        return "other";
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            return "other";
        }

        if ("EMAIL".equalsIgnoreCase(channel.trim())) {
            return "email";
        }
        return "other";
    }

    private String normalize(String value, Set<String> allowedValues, String fallback) {
        if (value == null) {
            return fallback;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        if (allowedValues.contains(normalizedValue)) {
            return normalizedValue;
        }
        return fallback;
    }

    private Duration nonNegative(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }
}

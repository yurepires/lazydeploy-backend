package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Métricas agregadas do ciclo de monitoramento.
 *
 * <p>Esta classe concentra os nomes e os valores permitidos das tags para
 * evitar que identificadores de servidores, usuários ou subscriptions sejam
 * registrados como cardinalidade de métricas.</p>
 */
@Component
public class MonitoringMetrics {

    private static final String CYCLE_COUNTER = "lazydeploy.monitoring.cycles";
    private static final String CYCLE_DURATION = "lazydeploy.monitoring.cycle.duration";
    private static final String SERVERS_COUNTER = "lazydeploy.monitoring.servers.processed";
    private static final String ACTIVE_SERVERS = "lazydeploy.monitoring.active_servers";
    private static final String ACTIVE_SUBSCRIPTIONS = "lazydeploy.monitoring.active_subscriptions";

    private static final Set<String> CYCLE_STATUSES = Set.of("success", "partial", "failed");
    private static final Set<String> SERVER_STATUSES = Set.of("success", "failed", "skipped");

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeServerCount = new AtomicInteger();
    private final AtomicInteger activeSubscriptionCount = new AtomicInteger();

    public MonitoringMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        registerGauges();
    }

    public static MonitoringMetrics noop() {
        return new MonitoringMetrics(null);
    }

    public void recordCycle(String status, Duration duration) {
        if (meterRegistry == null) {
            return;
        }

        try {
            String normalizedStatus = normalize(status, CYCLE_STATUSES, "failed");
            Counter.builder(CYCLE_COUNTER)
                    .tag("status", normalizedStatus)
                    .description("Quantidade de ciclos de monitoramento executados")
                    .register(meterRegistry)
                    .increment();
            Timer.builder(CYCLE_DURATION)
                    .description("Duração dos ciclos de monitoramento")
                    .register(meterRegistry)
                    .record(nonNegative(duration));
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper o monitoramento.
        }
    }

    public void recordServerProcessed(String status) {
        if (meterRegistry == null) {
            return;
        }

        try {
            String normalizedStatus = normalize(status, SERVER_STATUSES, "failed");
            Counter.builder(SERVERS_COUNTER)
                    .tag("status", normalizedStatus)
                    .description("Quantidade de servers processados no monitoramento")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper o monitoramento.
        }
    }

    public void setActiveCounts(int servers, int subscriptions) {
        activeServerCount.set(Math.max(servers, 0));
        activeSubscriptionCount.set(Math.max(subscriptions, 0));
    }

    private void registerGauges() {
        if (meterRegistry == null) {
            return;
        }

        try {
            Gauge.builder(ACTIVE_SERVERS, activeServerCount, AtomicInteger::get)
                    .description("Quantidade atual de servers ativos")
                    .register(meterRegistry);
            Gauge.builder(ACTIVE_SUBSCRIPTIONS, activeSubscriptionCount, AtomicInteger::get)
                    .description("Quantidade atual de subscriptions ativas")
                    .register(meterRegistry);
        } catch (RuntimeException ignored) {
            // A observabilidade nunca deve interromper a inicialização.
        }
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

package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityMetricsTest {

    @Test
    void shouldRecordMonitoringCycleAndActiveGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MonitoringMetrics metrics = new MonitoringMetrics(registry);

        metrics.recordCycle("success", Duration.ofMillis(25));
        metrics.setActiveCounts(2, 5);

        assertThat(registry.get("lazydeploy.monitoring.cycles")
                .tag("status", "success")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get("lazydeploy.monitoring.cycle.duration")
                .timer()
                .count()).isEqualTo(1);
        assertThat(registry.get("lazydeploy.monitoring.active_servers")
                .gauge()
                .value()).isEqualTo(2);
        assertThat(registry.get("lazydeploy.monitoring.active_subscriptions")
                .gauge()
                .value()).isEqualTo(5);
    }

    @Test
    void shouldUseOnlyControlledTagsForNotificationMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NotificationMetrics metrics = new NotificationMetrics(registry);

        metrics.recordRuleEvaluation("unexpected-server-guid", "matched");
        metrics.recordDelivery("EMAIL", "success", Duration.ofMillis(10));

        assertThat(registry.get("lazydeploy.rules.evaluations")
                .tag("type", "other")
                .tag("result", "matched")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get("lazydeploy.notification.delivery.attempts")
                .tag("channel", "email")
                .tag("outcome", "success")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allMatch(meter -> meter.getId().getTags().stream()
                        .noneMatch(tag -> tag.getValue().contains("unexpected-server-guid")));
    }
}

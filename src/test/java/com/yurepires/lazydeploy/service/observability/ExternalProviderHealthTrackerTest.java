package com.yurepires.lazydeploy.service.observability;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalProviderHealthTrackerTest {

    private static final Instant NOW = Instant.parse("2026-08-30T20:00:00Z");

    @Test
    void shouldReportUnknownBeforeTheFirstAttempt() {
        ExternalProviderHealthTracker tracker = tracker();

        ProviderHealthStatus status = tracker.getStatus("BATTLELOG_KEEPER");

        assertThat(status.hasAttempt()).isFalse();
        assertThat(status.consecutiveFailures()).isZero();
        assertThat(status.lastSuccessAt()).isNull();
        assertThat(status.lastFailureAt()).isNull();
    }

    @Test
    void shouldResetConsecutiveFailuresAfterSuccess() {
        ExternalProviderHealthTracker tracker = tracker();

        tracker.recordFailure("battlelog_keeper", "timeout");
        tracker.recordFailure("battlelog_keeper", "http_5xx");
        tracker.recordSuccess("battlelog_keeper");

        ProviderHealthStatus status = tracker.getStatus("BATTLELOG_KEEPER");

        assertThat(status.consecutiveFailures()).isZero();
        assertThat(status.lastFailureCategory()).isNull();
        assertThat(status.lastSuccessAt()).isEqualTo(NOW);
    }

    @Test
    void shouldKeepFailureCategoryWithoutStoringAnException() {
        ExternalProviderHealthTracker tracker = tracker();

        tracker.recordFailure("gametools", "connection_error");

        ProviderHealthStatus status = tracker.getStatus("GAMETOOLS");

        assertThat(status.lastFailureCategory()).isEqualTo("CONNECTION_ERROR");
        assertThat(status.lastFailureAt()).isEqualTo(NOW);
    }

    private ExternalProviderHealthTracker tracker() {
        return new ExternalProviderHealthTracker(
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}

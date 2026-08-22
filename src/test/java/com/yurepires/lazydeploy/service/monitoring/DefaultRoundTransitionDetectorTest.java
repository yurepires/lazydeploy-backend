package com.yurepires.lazydeploy.service.monitoring;


import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.server.MapSnapshot;
import com.yurepires.lazydeploy.model.server.PlayerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRoundTransitionDetectorTest {
    private final DefaultRoundTransitionDetector detector =
            new DefaultRoundTransitionDetector(30, Duration.ofMinutes(5));

    @Test
    void shouldDetectNewRoundWhenMapChanges() {
        assertThat(detector.isNewRound(state("MAP_A", 500, instant(0)), snapshot("MAP_B", 510, instant(30)))).isTrue();
    }

    @Test
    void shouldDetectNewRoundWhenRoundTimeResetsSignificantlyOnSameMap() {
        assertThat(detector.isNewRound(state("MAP_A", 1800, instant(0)), snapshot("MAP_A", 12, instant(30)))).isTrue();
    }

    @Test
    void shouldNotDetectNewRoundForSmallRoundTimeRegression() {
        assertThat(detector.isNewRound(state("MAP_A", 1800, instant(0)), snapshot("MAP_A", 1799, instant(30)))).isFalse();
    }

    @Test
    void shouldDetectNewRoundAfterLargeObservationGap() {
        assertThat(detector.isNewRound(state("MAP_A", 500, instant(0)), snapshot("MAP_A", 800, instant(301)))).isTrue();
    }

    private ServerState state(String map, long roundTime, Instant observedAt) {
        return new ServerState(UUID.randomUUID(), UUID.randomUUID(), map, roundTime, observedAt, observedAt);
    }

    private ServerSnapshot snapshot(String map, long roundTime, Instant capturedAt) {
        return new ServerSnapshot("guid", new MapSnapshot(map, map, null),
                new PlayerSnapshot(10, 64, 0), "CONQUEST", roundTime, capturedAt, Map.of());
    }

    private Instant instant(long seconds) {
        return Instant.parse("2026-08-13T12:00:00Z").plusSeconds(seconds);
    }
}

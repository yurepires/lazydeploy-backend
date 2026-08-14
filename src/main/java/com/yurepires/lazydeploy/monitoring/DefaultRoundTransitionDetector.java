package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.monitoring.RoundTransitionDetector;
import com.yurepires.lazydeploy.domain.monitoring.ServerState;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Objects;

@Component
public class DefaultRoundTransitionDetector implements RoundTransitionDetector {
    private final long resetThresholdSeconds;
    private final Duration maxObservationGap;

    @Autowired
    public DefaultRoundTransitionDetector(LazyDeployProperties properties) {
        this(properties.monitoring().roundResetThresholdSeconds(), properties.monitoring().maxObservationGap());
    }

    public DefaultRoundTransitionDetector(long resetThresholdSeconds, Duration maxObservationGap) {
        this.resetThresholdSeconds = resetThresholdSeconds;
        this.maxObservationGap = maxObservationGap;
    }

    @Override
    public boolean isNewRound(ServerState previous, ServerSnapshot current) {
        if (!Objects.equals(previous.mapId(), current.map().normalizedId())) {
            return true;
        }
        if (previous.previousRoundTimeSeconds() - current.roundTimeSeconds() >= resetThresholdSeconds) {
            return true;
        }
        return maxObservationGap != null
                && Duration.between(previous.lastObservedAt(), current.capturedAt()).compareTo(maxObservationGap) > 0;
    }
}

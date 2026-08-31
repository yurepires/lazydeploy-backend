package com.yurepires.lazydeploy.service.observability;

import java.time.Instant;
import java.util.UUID;

/**
 * Contexto transitório usado para correlacionar os logs de uma execução do
 * scheduler. Não é persistido nem utilizado como tag de métrica.
 */
public final class MonitoringCycleContext {

    private final UUID cycleId;
    private final Instant startedAt;

    private MonitoringCycleContext(UUID cycleId, Instant startedAt) {
        this.cycleId = cycleId;
        this.startedAt = startedAt;
    }

    public static MonitoringCycleContext start() {
        return new MonitoringCycleContext(UUID.randomUUID(), Instant.now());
    }

    public UUID cycleId() {
        return cycleId;
    }

    public Instant startedAt() {
        return startedAt;
    }
}

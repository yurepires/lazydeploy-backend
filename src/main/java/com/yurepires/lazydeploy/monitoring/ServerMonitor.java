package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.monitoring.RoundInstance;
import com.yurepires.lazydeploy.domain.monitoring.RoundTransitionDetector;
import com.yurepires.lazydeploy.domain.monitoring.ServerState;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerReference;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.notification.NotificationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServerMonitor {
    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);

    private final ServerSnapshotProvider snapshotProvider;
    private final LazyDeployProperties properties;
    private final RoundTransitionDetector transitionDetector;
    private final NotificationOrchestrator orchestrator;
    private final Map<String, ServerState> serverStates = new ConcurrentHashMap<>();

    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            LazyDeployProperties properties,
            RoundTransitionDetector transitionDetector,
            NotificationOrchestrator orchestrator
    ) {
        this.snapshotProvider = snapshotProvider;
        this.properties = properties;
        this.transitionDetector = transitionDetector;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${lazydeploy.monitoring.interval}")
    public void monitor() {
        for (MonitoredServer configured : properties.servers()) {
            if (!configured.enabled()) {
                continue;
            }
            try {
                monitorServer(configured);
            } catch (RuntimeException exception) {
                log.error("Erro ao monitorar serverId={}", configured.id(), exception);
            }
        }
    }

    private void monitorServer(MonitoredServer configured) {
        if (configured.identifiers() == null || !configured.identifiers().hasGuid()) {
            log.warn("Servidor sem GUID | serverId={}", configured.id());
            return;
        }
        ServerReference reference = new ServerReference(
                null, configured.identifiers().guid(), configured.displayName(),
                snapshotProvider.providerId(), Map.of("configuredServerId", configured.id())
        );
        ServerSnapshot current = snapshotProvider.getSnapshot(reference).orElse(null);
        if (current == null) {
            log.warn("Snapshot indisponivel | serverId={} | guid={}", configured.id(), reference.externalGuid());
            return;
        }

        ServerState previous = serverStates.get(current.serverGuid());
        boolean newRound = previous == null || transitionDetector.isNewRound(previous, current);
        RoundInstance round = newRound
                ? RoundInstance.detected(current.serverGuid(), current.map().normalizedId(), current.capturedAt(), current.roundTimeSeconds())
                : new RoundInstance(previous.roundInstanceId(), previous.serverGuid(), previous.mapId(), previous.roundDetectedAt(), previous.previousRoundTimeSeconds());
        ServerState observed = new ServerState(
                current.serverGuid(), round.id(), current.map().normalizedId(), current.roundTimeSeconds(),
                round.detectedAt(), current.capturedAt()
        );
        serverStates.put(current.serverGuid(), observed);

        if (previous == null) {
            orchestrator.initialize(current.serverGuid(), round.id());
            log.info("Estado inicial registrado | serverId={} | roundInstanceId={}", configured.id(), round.id());
            return;
        }
        if (newRound) {
            log.info("ROUND CHANGE | serverId={} | previous={} | current={} | map={}",
                    configured.id(), previous.roundInstanceId(), round.id(), current.map().normalizedId());
        }
        orchestrator.process(configured, current, previous, observed, current.capturedAt());
    }
}

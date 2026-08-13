package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.battlefield.BfListSnapshotService;
import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.monitoring.ServerState;
import com.yurepires.lazydeploy.domain.monitoring.StateIdentityStrategy;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerCatalogSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerLocator;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.notification.NotificationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServerMonitor {

    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);

    private final BfListSnapshotService snapshotService;
    private final LazyDeployProperties properties;
    private final ServerLocator serverLocator;
    private final StateIdentityStrategy identityStrategy;
    private final NotificationOrchestrator orchestrator;
    private final Map<String, ServerState> serverStates = new ConcurrentHashMap<>();

    public ServerMonitor(
            BfListSnapshotService snapshotService,
            LazyDeployProperties properties,
            ServerLocator serverLocator,
            StateIdentityStrategy identityStrategy,
            NotificationOrchestrator orchestrator
    ) {
        this.snapshotService = snapshotService;
        this.properties = properties;
        this.serverLocator = serverLocator;
        this.identityStrategy = identityStrategy;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${lazydeploy.monitoring.interval}")
    public void monitor() {
        ServerCatalogSnapshot catalog = snapshotService.fetchSnapshot().orElse(null);
        if (catalog == null) {
            log.warn("Ciclo abortado: snapshot do BFLIST indisponível.");
            return;
        }

        for (MonitoredServer configured : properties.servers()) {
            if (!configured.enabled()) {
                continue;
            }
            try {
                monitorServer(configured, catalog);
            } catch (RuntimeException exception) {
                log.error("Erro ao monitorar serverId={}", configured.id(), exception);
            }
        }
    }

    private void monitorServer(MonitoredServer configured, ServerCatalogSnapshot catalog) {
        ServerSnapshot current = serverLocator.find(configured, catalog).orElse(null);
        if (current == null) {
            log.warn("Servidor não encontrado | serverId={} | displayName='{}'", configured.id(), configured.displayName());
            return;
        }

        Instant now = Instant.now();
        ServerState previous = serverStates.get(configured.id());
        String identity = identityStrategy.createIdentity(configured, current, previous, now);
        ServerState observed = new ServerState(
                configured.id(),
                current,
                identity,
                previous == null || !previous.stateIdentity().equals(identity) ? now : previous.firstObservedAt(),
                now
        );
        serverStates.put(configured.id(), observed);

        if (previous == null) {
            orchestrator.initialize(configured, identity);
            log.info("Estado inicial registrado | serverId={} | state={}", configured.id(), identity);
            return;
        }

        if (!previous.stateIdentity().equals(identity)) {
            log.info(
                    "STATE CHANGE | serverId={} | previous={} | current={}",
                    configured.id(), previous.stateIdentity(), identity
            );
        }

        orchestrator.process(configured, current, previous, identity, now);
    }
}

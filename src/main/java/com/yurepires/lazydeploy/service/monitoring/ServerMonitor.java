package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.service.notification.NotificationOrchestrator;
import com.yurepires.lazydeploy.service.subscription.ServerSubscriptionPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ServerMonitor {

    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);

    private final ServerSnapshotProvider snapshotProvider;
    private final ServerSubscriptionPersistenceService subscriptionPersistenceService;
    private final MonitoringStateService monitoringStateService;
    private final NotificationOrchestrator notificationOrchestrator;
    private final ServerSnapshotEnricher snapshotEnricher;

    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            MonitoringStateService monitoringStateService,
            NotificationOrchestrator notificationOrchestrator
    ) {
        this(
                snapshotProvider,
                subscriptionPersistenceService,
                monitoringStateService,
                notificationOrchestrator,
                new ServerSnapshotEnricher()
        );
    }

    @Autowired
    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            MonitoringStateService monitoringStateService,
            NotificationOrchestrator notificationOrchestrator,
            ServerSnapshotEnricher snapshotEnricher
    ) {
        this.snapshotProvider = snapshotProvider;
        this.subscriptionPersistenceService = subscriptionPersistenceService;
        this.monitoringStateService = monitoringStateService;
        this.notificationOrchestrator = notificationOrchestrator;
        this.snapshotEnricher = snapshotEnricher;
    }

    @Scheduled(fixedDelayString = "${lazydeploy.monitoring.interval}")
    public void monitor() {
        Map<UUID, List<ServerSubscription>> subscriptionsByServer = groupEnabledSubscriptions();

        for (List<ServerSubscription> serverSubscriptions : subscriptionsByServer.values()) {
            monitorServerSafely(serverSubscriptions);
        }
    }

    private Map<UUID, List<ServerSubscription>> groupEnabledSubscriptions() {
        return subscriptionPersistenceService.findAllEnabled()
                .stream()
                .filter(subscription -> subscription.server().enabled())
                .collect(Collectors.groupingBy(
                        subscription -> subscription.server().id(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private void monitorServerSafely(List<ServerSubscription> serverSubscriptions) {
        Server server = serverSubscriptions.getFirst().server();

        try {
            monitorServer(server, serverSubscriptions);
        } catch (RuntimeException exception) {
            log.error("Erro ao monitorar serverId={}", server.id(), exception);
        }
    }

    private void monitorServer(
            Server server,
            List<ServerSubscription> serverSubscriptions
    ) {
        String serverGuid = serverSubscriptions.getFirst().externalGuid();
        ServerReference serverReference = createServerReference(server, serverGuid);
        Optional<ServerSnapshot> snapshot = snapshotProvider.getSnapshot(serverReference);

        if (snapshot.isEmpty()) {
            log.warn("Snapshot indisponível | serverId={} | guid={}", server.id(), serverGuid);
            return;
        }

        ServerSnapshot enrichedSnapshot = snapshotEnricher.enrich(snapshot.get());
        processSnapshot(server, serverSubscriptions, enrichedSnapshot);
    }

    private ServerReference createServerReference(Server server, String serverGuid) {
        return new ServerReference(
                server.id(),
                serverGuid,
                server.displayName(),
                snapshotProvider.providerId(),
                Map.of()
        );
    }

    private void processSnapshot(
            Server server,
            List<ServerSubscription> serverSubscriptions,
            ServerSnapshot currentSnapshot
    ) {
        MonitoringObservation observation = monitoringStateService.observe(
                server,
                currentSnapshot
        );

        if (observation.initial()) {
            initializeSubscriptions(server, serverSubscriptions, observation);
            return;
        }

        if (observation.newRound()) {
            logRoundChange(server, currentSnapshot, observation);
        }

        evaluateSubscriptions(serverSubscriptions, currentSnapshot, observation);
    }

    private void initializeSubscriptions(
            Server server,
            List<ServerSubscription> serverSubscriptions,
            MonitoringObservation observation
    ) {
        UUID roundInstanceId = observation.current().roundInstanceId();

        for (ServerSubscription subscription : serverSubscriptions) {
            notificationOrchestrator.initialize(subscription.id(), roundInstanceId);
        }

        log.info(
                "Estado inicial persistido | serverId={} | roundInstanceId={}",
                server.id(),
                roundInstanceId
        );
    }

    private void logRoundChange(
            Server server,
            ServerSnapshot currentSnapshot,
            MonitoringObservation observation
    ) {
        log.info(
                "ROUND CHANGE | serverId={} | previous={} | current={} | map={}",
                server.id(),
                observation.previous().roundInstanceId(),
                observation.current().roundInstanceId(),
                currentSnapshot.map().normalizedId()
        );
    }

    private void evaluateSubscriptions(
            List<ServerSubscription> serverSubscriptions,
            ServerSnapshot currentSnapshot,
            MonitoringObservation observation
    ) {
        for (ServerSubscription subscription : serverSubscriptions) {
            evaluateSubscriptionSafely(subscription, currentSnapshot, observation);
        }
    }

    private void evaluateSubscriptionSafely(
            ServerSubscription subscription,
            ServerSnapshot currentSnapshot,
            MonitoringObservation observation
    ) {
        try {
            notificationOrchestrator.process(
                    subscription.toMonitoredServer(),
                    currentSnapshot,
                    observation.previous(),
                    observation.current(),
                    currentSnapshot.capturedAt()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Erro ao avaliar subscriptionId={}",
                    subscription.id(),
                    exception
            );
        }
    }
}

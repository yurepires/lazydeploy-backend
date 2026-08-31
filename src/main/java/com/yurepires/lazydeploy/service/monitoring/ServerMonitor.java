package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.service.notification.NotificationOrchestrator;
import com.yurepires.lazydeploy.service.observability.MonitoringCycleContext;
import com.yurepires.lazydeploy.service.observability.MonitoringMetrics;
import com.yurepires.lazydeploy.service.subscription.ServerSubscriptionPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
    private final MonitoringMetrics metrics;

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
                new ServerSnapshotEnricher(),
                MonitoringMetrics.noop()
        );
    }

    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            MonitoringStateService monitoringStateService,
            NotificationOrchestrator notificationOrchestrator,
            ServerSnapshotEnricher snapshotEnricher
    ) {
        this(
                snapshotProvider,
                subscriptionPersistenceService,
                monitoringStateService,
                notificationOrchestrator,
                snapshotEnricher,
                MonitoringMetrics.noop()
        );
    }

    @Autowired
    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            MonitoringStateService monitoringStateService,
            NotificationOrchestrator notificationOrchestrator,
            ServerSnapshotEnricher snapshotEnricher,
            MonitoringMetrics metrics
    ) {
        this.snapshotProvider = snapshotProvider;
        this.subscriptionPersistenceService = subscriptionPersistenceService;
        this.monitoringStateService = monitoringStateService;
        this.notificationOrchestrator = notificationOrchestrator;
        this.snapshotEnricher = snapshotEnricher;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${lazydeploy.monitoring.interval}")
    public void monitor() {
        MonitoringCycleContext cycleContext = MonitoringCycleContext.start();
        long startedAt = System.nanoTime();
        String cycleStatus = "success";
        MDC.put("cycleId", cycleContext.cycleId().toString());

        try {
            Map<UUID, List<ServerSubscription>> subscriptionsByServer = groupEnabledSubscriptions();
            int activeSubscriptionCount = subscriptionsByServer.values().stream()
                    .mapToInt(List::size)
                    .sum();
            metrics.setActiveCounts(subscriptionsByServer.size(), activeSubscriptionCount);

            for (List<ServerSubscription> serverSubscriptions : subscriptionsByServer.values()) {
                ServerProcessingOutcome outcome = monitorServerSafely(serverSubscriptions);
                metrics.recordServerProcessed(outcome.metricValue());
                if (outcome == ServerProcessingOutcome.FAILED) {
                    cycleStatus = "partial";
                }
            }
        } catch (RuntimeException exception) {
            cycleStatus = "failed";
            log.error(
                    "Falha global no ciclo de monitoramento | exceptionType={}",
                    exception.getClass().getSimpleName()
            );
        } finally {
            metrics.recordCycle(
                    cycleStatus,
                    Duration.ofNanos(System.nanoTime() - startedAt)
            );
            log.debug(
                    "MONITORING CYCLE | cycleId={} | status={}",
                    cycleContext.cycleId(),
                    cycleStatus
            );
            MDC.remove("cycleId");
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

    private ServerProcessingOutcome monitorServerSafely(
            List<ServerSubscription> serverSubscriptions
    ) {
        Server server = serverSubscriptions.getFirst().server();

        try {
            boolean processed = monitorServer(server, serverSubscriptions);
            if (processed) {
                return ServerProcessingOutcome.SUCCESS;
            }
            return ServerProcessingOutcome.FAILED;
        } catch (RuntimeException exception) {
            log.error(
                    "Erro ao monitorar serverId={} | exceptionType={}",
                    server.id(),
                    exception.getClass().getSimpleName()
            );
            return ServerProcessingOutcome.FAILED;
        }
    }

    private boolean monitorServer(
            Server server,
            List<ServerSubscription> serverSubscriptions
    ) {
        String serverGuid = serverSubscriptions.getFirst().externalGuid();
        ServerReference serverReference = createServerReference(server, serverGuid);
        Optional<ServerSnapshot> snapshot = snapshotProvider.getSnapshot(serverReference);

        if (snapshot.isEmpty()) {
            log.warn("Snapshot indisponível | serverId={} | guid={}", server.id(), serverGuid);
            return false;
        }

        ServerSnapshot enrichedSnapshot = snapshotEnricher.enrich(snapshot.get());
        return processSnapshot(server, serverSubscriptions, enrichedSnapshot);
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

    private boolean processSnapshot(
            Server server,
            List<ServerSubscription> serverSubscriptions,
            ServerSnapshot currentSnapshot
    ) {
        MonitoringObservation observation = monitoringStateService.observe(
                server,
                currentSnapshot
        );

        if (observation.initial()) {
            return initializeSubscriptions(server, serverSubscriptions, observation);
        }

        if (observation.newRound()) {
            logRoundChange(server, currentSnapshot, observation);
        }

        return evaluateSubscriptions(serverSubscriptions, currentSnapshot, observation);
    }

    private boolean initializeSubscriptions(
            Server server,
            List<ServerSubscription> serverSubscriptions,
            MonitoringObservation observation
    ) {
        UUID roundInstanceId = observation.current().roundInstanceId();
        boolean allSubscriptionsProcessed = true;

        for (ServerSubscription subscription : serverSubscriptions) {
            try {
                notificationOrchestrator.initialize(subscription.id(), roundInstanceId);
            } catch (RuntimeException exception) {
                allSubscriptionsProcessed = false;
                log.error(
                        "Erro ao inicializar subscriptionId={} | exceptionType={}",
                        subscription.id(),
                        exception.getClass().getSimpleName()
                );
            }
        }

        log.info(
                "Estado inicial persistido | serverId={} | roundInstanceId={}",
                server.id(),
                roundInstanceId
        );
        return allSubscriptionsProcessed;
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

    private boolean evaluateSubscriptions(
            List<ServerSubscription> serverSubscriptions,
            ServerSnapshot currentSnapshot,
            MonitoringObservation observation
    ) {
        boolean allSubscriptionsProcessed = true;

        for (ServerSubscription subscription : serverSubscriptions) {
            if (!evaluateSubscriptionSafely(subscription, currentSnapshot, observation)) {
                allSubscriptionsProcessed = false;
            }
        }

        return allSubscriptionsProcessed;
    }

    private boolean evaluateSubscriptionSafely(
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
            return true;
        } catch (RuntimeException exception) {
            log.error(
                    "Erro ao avaliar subscriptionId={} | exceptionType={}",
                    subscription.id(),
                    exception.getClass().getSimpleName()
            );
            return false;
        }
    }

    private enum ServerProcessingOutcome {
        SUCCESS("success"),
        FAILED("failed");

        private final String metricValue;

        ServerProcessingOutcome(String metricValue) {
            this.metricValue = metricValue;
        }

        public String metricValue() {
            return metricValue;
        }
    }
}

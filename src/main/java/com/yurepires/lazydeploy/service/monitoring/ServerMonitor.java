package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.service.notification.NotificationOrchestrator;
import com.yurepires.lazydeploy.service.observability.MonitoringCycleContext;
import com.yurepires.lazydeploy.service.observability.MonitoringMetrics;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import com.yurepires.lazydeploy.service.observability.NotificationMetrics;
import com.yurepires.lazydeploy.service.subscription.ServerSubscriptionPersistenceService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final NotificationMetrics notificationMetrics;
    private final TaskExecutor monitoringExecutor;
    private final LogSanitizer logSanitizer;
    private final AtomicBoolean cycleInProgress = new AtomicBoolean();
    private final AtomicBoolean shuttingDown = new AtomicBoolean();

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
                MonitoringMetrics.noop(),
                NotificationMetrics.noop(),
                null,
                new LogSanitizer()
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
                MonitoringMetrics.noop(),
                NotificationMetrics.noop(),
                null,
                new LogSanitizer()
        );
    }

    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            MonitoringStateService monitoringStateService,
            NotificationOrchestrator notificationOrchestrator,
            ServerSnapshotEnricher snapshotEnricher,
            MonitoringMetrics metrics
    ) {
        this(
                snapshotProvider,
                subscriptionPersistenceService,
                monitoringStateService,
                notificationOrchestrator,
                snapshotEnricher,
                metrics,
                NotificationMetrics.noop(),
                null,
                new LogSanitizer()
        );
    }

    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            MonitoringStateService monitoringStateService,
            NotificationOrchestrator notificationOrchestrator,
            ServerSnapshotEnricher snapshotEnricher,
            MonitoringMetrics metrics,
            TaskExecutor monitoringExecutor
    ) {
        this(
                snapshotProvider,
                subscriptionPersistenceService,
                monitoringStateService,
                notificationOrchestrator,
                snapshotEnricher,
                metrics,
                NotificationMetrics.noop(),
                monitoringExecutor,
                new LogSanitizer()
        );
    }

    @Autowired
    public ServerMonitor(
            ServerSnapshotProvider snapshotProvider,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            MonitoringStateService monitoringStateService,
            NotificationOrchestrator notificationOrchestrator,
            ServerSnapshotEnricher snapshotEnricher,
            MonitoringMetrics metrics,
            NotificationMetrics notificationMetrics,
            @Qualifier("monitoringTaskExecutor") TaskExecutor monitoringExecutor,
            LogSanitizer logSanitizer
    ) {
        this.snapshotProvider = snapshotProvider;
        this.subscriptionPersistenceService = subscriptionPersistenceService;
        this.monitoringStateService = monitoringStateService;
        this.notificationOrchestrator = notificationOrchestrator;
        this.snapshotEnricher = snapshotEnricher;
        this.metrics = metrics;
        this.notificationMetrics = notificationMetrics;
        this.monitoringExecutor = monitoringExecutor;
        this.logSanitizer = logSanitizer;
    }

    @Scheduled(fixedDelayString = "${lazydeploy.monitoring.interval}")
    public void monitor() {
        if (shuttingDown.get()) {
            metrics.recordCycleSkipped();
            log.debug("MONITORING CYCLE SKIPPED | reason=SHUTDOWN");
            return;
        }

        if (!cycleInProgress.compareAndSet(false, true)) {
            metrics.recordCycleSkipped();
            log.warn(
                    "MONITORING CYCLE SKIPPED | reason=PREVIOUS_CYCLE_STILL_RUNNING"
            );
            return;
        }

        if (monitoringExecutor == null) {
            runMonitoringCycleAndReleaseGuard();
            return;
        }

        try {
            monitoringExecutor.execute(this::runMonitoringCycleAndReleaseGuard);
        } catch (RejectedExecutionException exception) {
            cycleInProgress.set(false);
            metrics.recordCycleSkipped();
            log.warn(
                    "MONITORING CYCLE SKIPPED | reason=EXECUTOR_REJECTED"
            );
        }
    }

    @PreDestroy
    public void stopSchedulingNewCycles() {
        shuttingDown.set(true);
    }

    private void runMonitoringCycleAndReleaseGuard() {
        try {
            runMonitoringCycle();
        } finally {
            cycleInProgress.set(false);
        }
    }

    private void runMonitoringCycle() {
        MonitoringCycleContext cycleContext = MonitoringCycleContext.start();
        long startedAt = System.nanoTime();
        String cycleStatus = "success";
        CycleStatistics statistics = new CycleStatistics();
        long deliveriesBeforeCycle = notificationMetrics.successfulDeliveryCount();
        MDC.put("cycleId", cycleContext.cycleId().toString());

        try {
            Map<UUID, List<ServerSubscription>> subscriptionsByServer = groupEnabledSubscriptions();
            int activeSubscriptionCount = subscriptionsByServer.values().stream()
                    .mapToInt(List::size)
                    .sum();
            metrics.setActiveCounts(subscriptionsByServer.size(), activeSubscriptionCount);

            for (List<ServerSubscription> serverSubscriptions : subscriptionsByServer.values()) {
                statistics.serversProcessed++;
                ServerProcessingOutcome outcome = monitorServerSafely(
                        serverSubscriptions,
                        statistics
                );
                metrics.recordServerProcessed(outcome.metricValue());
                if (outcome == ServerProcessingOutcome.FAILED) {
                    cycleStatus = "partial";
                }
            }
        } catch (RuntimeException exception) {
            cycleStatus = "failed";
            log.error(
                    "Falha global no ciclo de monitoramento | exceptionType={}",
                    exception.getClass().getSimpleName(),
                    exception
            );
        } finally {
            statistics.notificationsSent = (int) Math.max(
                    0,
                    notificationMetrics.successfulDeliveryCount() - deliveriesBeforeCycle
            );
            metrics.recordCycle(
                    cycleStatus,
                    Duration.ofNanos(System.nanoTime() - startedAt)
            );
            log.info(
                    "MONITORING CYCLE | cycleId={} | serversProcessed={} | "
                            + "serversSucceeded={} | serversFailed={} | "
                            + "subscriptionsEvaluated={} | notificationsSent={} | "
                            + "durationMs={} | status={}",
                    cycleContext.cycleId(),
                    statistics.serversProcessed,
                    statistics.serversSucceeded,
                    statistics.serversFailed,
                    statistics.subscriptionsEvaluated,
                    statistics.notificationsSent,
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis(),
                    cycleStatus.toUpperCase(Locale.ROOT)
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
            List<ServerSubscription> serverSubscriptions,
            CycleStatistics statistics
    ) {
        Server server = serverSubscriptions.getFirst().server();

        try {
            boolean processed = monitorServer(server, serverSubscriptions, statistics);
            if (processed) {
                statistics.serversSucceeded++;
                return ServerProcessingOutcome.SUCCESS;
            }
            statistics.serversFailed++;
            return ServerProcessingOutcome.FAILED;
        } catch (RuntimeException exception) {
            statistics.serversFailed++;
            log.error(
                    "Erro ao monitorar server | exceptionType={}",
                    exception.getClass().getSimpleName(),
                    exception
            );
            return ServerProcessingOutcome.FAILED;
        }
    }

    private boolean monitorServer(
            Server server,
            List<ServerSubscription> serverSubscriptions,
            CycleStatistics statistics
    ) {
        String serverGuid = serverSubscriptions.getFirst().externalGuid();
        ServerReference serverReference = createServerReference(server, serverGuid);
        Optional<ServerSnapshot> snapshot = snapshotProvider.getSnapshot(serverReference);

        if (snapshot.isEmpty()) {
            log.warn("SNAPSHOT UNAVAILABLE | provider={}", snapshotProvider.providerId());
            return false;
        }

        ServerSnapshot enrichedSnapshot = snapshotEnricher.enrich(snapshot.get());
        return processSnapshot(server, serverSubscriptions, enrichedSnapshot, statistics);
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
            ServerSnapshot currentSnapshot,
            CycleStatistics statistics
    ) {
        MonitoringObservation observation = monitoringStateService.observe(
                server,
                currentSnapshot
        );

        if (observation.initial()) {
            return initializeSubscriptions(server, serverSubscriptions, observation);
        }

        if (observation.newRound()) {
            logRoundChange(currentSnapshot);
        }

        return evaluateSubscriptions(
                serverSubscriptions,
                currentSnapshot,
                observation,
                statistics
        );
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
                        "Erro ao inicializar subscription | exceptionType={}",
                        exception.getClass().getSimpleName(),
                        exception
                );
            }
        }

        log.debug(
                "INITIAL MONITORING STATE PERSISTED | subscriptions={}",
                serverSubscriptions.size()
        );
        return allSubscriptionsProcessed;
    }

    private void logRoundChange(
            ServerSnapshot currentSnapshot
    ) {
        log.info(
                "ROUND CHANGE | map={}",
                logSanitizer.sanitize(currentSnapshot.map().normalizedId())
        );
    }

    private boolean evaluateSubscriptions(
            List<ServerSubscription> serverSubscriptions,
            ServerSnapshot currentSnapshot,
            MonitoringObservation observation,
            CycleStatistics statistics
    ) {
        boolean allSubscriptionsProcessed = true;

        for (ServerSubscription subscription : serverSubscriptions) {
            statistics.subscriptionsEvaluated++;
            if (!evaluateSubscriptionSafely(subscription, currentSnapshot, observation, statistics)) {
                allSubscriptionsProcessed = false;
            }
        }

        return allSubscriptionsProcessed;
    }

    private boolean evaluateSubscriptionSafely(
            ServerSubscription subscription,
            ServerSnapshot currentSnapshot,
            MonitoringObservation observation,
            CycleStatistics statistics
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
                    "Erro ao avaliar subscription | exceptionType={}",
                    exception.getClass().getSimpleName(),
                    exception
            );
            return false;
        }
    }

    private static final class CycleStatistics {
        private int serversProcessed;
        private int serversSucceeded;
        private int serversFailed;
        private int subscriptionsEvaluated;
        private int notificationsSent;
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

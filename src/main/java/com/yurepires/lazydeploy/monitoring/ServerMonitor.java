package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.battlefield.Bf4ServerSnapshot;
import com.yurepires.lazydeploy.battlefield.BfListSnapshotService;
import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import com.yurepires.lazydeploy.monitoring.event.MapChangedEvent;
import com.yurepires.lazydeploy.notification.LoggingNotificationService;
import com.yurepires.lazydeploy.notification.NotificationCandidate;
import com.yurepires.lazydeploy.notification.rule.NotificationDecision;
import com.yurepires.lazydeploy.notification.rule.NotificationDecisionReason;
import com.yurepires.lazydeploy.notification.rule.NotificationRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServerMonitor {

    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);

    private final BfListSnapshotService snapshotService;
    private final Bf4Properties properties;
    private final NotificationRuleService notificationRuleService;
    private final LoggingNotificationService notificationService;
    private final Map<String, ServerState> previousStates = new ConcurrentHashMap<>();

    public ServerMonitor(
            BfListSnapshotService snapshotService,
            Bf4Properties properties,
            NotificationRuleService notificationRuleService,
            LoggingNotificationService notificationService
    ) {
        this.snapshotService = snapshotService;
        this.properties = properties;
        this.notificationRuleService = notificationRuleService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${bf4.monitoring.interval}")
    public void monitor() {
        try {
            Bf4ServerSnapshot snapshot = snapshotService.fetchSnapshot().orElse(null);
            if (snapshot == null) {
                log.warn("Ciclo de monitoramento abortado: snapshot do BFLIST indisponível.");
                return;
            }

            for (Bf4Properties.MonitoredServer target : properties.monitoredServers()) {
                if (!target.enabled()) {
                    continue;
                }

                try {
                    monitorServer(snapshot, target);
                } catch (RuntimeException exception) {
                    log.error(
                            "Erro ao monitorar servidor | name='{}' | guid={}",
                            target.name(),
                            target.guid(),
                            exception
                    );
                }
            }
        } catch (RuntimeException exception) {
            log.error("Erro inesperado no ciclo de monitoramento BF4.", exception);
        }
    }

    private void monitorServer(
            Bf4ServerSnapshot snapshot,
            Bf4Properties.MonitoredServer target
    ) {
        Bf4ServerResponse server = snapshot.find(target).orElse(null);
        if (server == null) {
            log.warn(
                    "Servidor monitorado não encontrado | name='{}' | guid={} | address={}:{}",
                    target.name(),
                    target.guid(),
                    target.ip(),
                    target.port()
            );
            return;
        }

        log.info(
                "Server='{}' | Map='{}' | Players={}/{} | RoundTime={}s",
                server.name(),
                server.mapLabel(),
                server.numPlayers(),
                server.maxPlayers(),
                server.roundTime()
        );

        ServerState currentState = new ServerState(server.map(), server.mapLabel());
        ServerState previousState = previousStates.put(server.guid(), currentState);

        if (previousState == null) {
            log.info(
                    "Estado inicial registrado | server='{}' | map='{}'",
                    server.name(),
                    server.mapLabel()
            );
            return;
        }

        if (Objects.equals(previousState.map(), currentState.map())) {
            return;
        }

        MapChangedEvent event = createMapChangedEvent(server, previousState);
        log.info(
                "MAP CHANGE | server='{}' | previous='{}' | current='{}' | players={}/{}",
                event.serverName(),
                event.previousMapLabel(),
                event.currentMapLabel(),
                event.numPlayers(),
                event.maxPlayers()
        );

        NotificationDecision decision = notificationRuleService.evaluate(target, event);
        logDecision(target, event, decision);

        if (decision.shouldNotify()) {
            notificationService.notify(toCandidate(event));
        }
    }

    private MapChangedEvent createMapChangedEvent(
            Bf4ServerResponse server,
            ServerState previousState
    ) {
        return new MapChangedEvent(
                server.guid(),
                server.name(),
                server.ip(),
                server.port(),
                previousState.map(),
                previousState.mapLabel(),
                server.map(),
                server.mapLabel(),
                server.numPlayers(),
                server.maxPlayers(),
                server.gameType(),
                server.roundTime(),
                Instant.now()
        );
    }

    private void logDecision(
            Bf4Properties.MonitoredServer target,
            MapChangedEvent event,
            NotificationDecision decision
    ) {
        if (decision.reason() == NotificationDecisionReason.MAP_NOT_FAVORITE) {
            log.info(
                    "NOTIFICATION SKIPPED | server='{}' | map='{}' | reason={}",
                    event.serverName(),
                    event.currentMapLabel(),
                    decision.reason()
            );
        } else if (decision.reason() == NotificationDecisionReason.MIN_PLAYERS_NOT_REACHED) {
            log.info(
                    "NOTIFICATION SKIPPED | server='{}' | map='{}' | players={} | minimum={} | reason={}",
                    event.serverName(),
                    event.currentMapLabel(),
                    event.numPlayers(),
                    target.minPlayers(),
                    decision.reason()
            );
        } else if (decision.shouldNotify()) {
            log.info(
                    "NOTIFICATION APPROVED | server='{}' | map='{}' | players={}/{}",
                    event.serverName(),
                    event.currentMapLabel(),
                    event.numPlayers(),
                    event.maxPlayers()
            );
        } else {
            log.info(
                    "NOTIFICATION SKIPPED | server='{}' | reason={}",
                    event.serverName(),
                    decision.reason()
            );
        }
    }

    private NotificationCandidate toCandidate(MapChangedEvent event) {
        return new NotificationCandidate(
                event.serverGuid(),
                event.serverName(),
                event.currentMap(),
                event.currentMapLabel(),
                event.numPlayers(),
                event.maxPlayers(),
                event.gameType(),
                event.detectedAt()
        );
    }
}

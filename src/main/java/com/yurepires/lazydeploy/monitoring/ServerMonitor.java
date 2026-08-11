package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.battlefield.ServerDiscoveryService;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
public class ServerMonitor {

    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);
    private final ServerDiscoveryService serverDiscoveryService;
    private String previousMap;
    private String previousMapLabel;

    public ServerMonitor(ServerDiscoveryService serverDiscoveryService) {
        this.serverDiscoveryService = serverDiscoveryService;
    }

    @Scheduled(fixedDelayString = "${bf4.monitoring.interval}")
    public void monitor() {
        try {
            Bf4ServerResponse server = serverDiscoveryService.findTrackedServer().orElse(null);

            if (server == null) {
                log.warn("Servidor configurado não foi encontrado no snapshot atual do BFLIST.");
                return;
            }

            logServerStatus(server);
            detectMapChange(server);
        } catch (Exception exception) {
            log.error("Erro ao consultar servidor BF4: ", exception);
        }
    }

    private void logServerStatus(Bf4ServerResponse server) {
        log.info(
                "Server='{}' | Map='{}' | Players={}/{} | RoundTime={}s",
                server.name(),
                server.mapLabel(),
                server.numPlayers(),
                server.maxPlayers(),
                server.roundTime()
        );
    }

    private void detectMapChange(Bf4ServerResponse server) {
        if (previousMap == null) {
            previousMap = server.map();
            previousMapLabel = server.mapLabel();

            log.info("Estado inicial registrado: {}", server.mapLabel());

            return;
        }

        if (!Objects.equals(previousMap, server.map())) {
            log.info(
                "MAP CHANGE DETECTED | Previous='{}' | Current='{}' | Players={}/{}",
                previousMapLabel,
                server.mapLabel(),
                server.numPlayers(),
                server.maxPlayers()
            );

            previousMap = server.map();
            previousMapLabel = server.mapLabel();
        }
    }
}

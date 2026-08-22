package com.yurepires.lazydeploy.integration.battlelogkeeper;

import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;
import java.time.Duration;

@Component
public class BattlelogKeeperSnapshotProvider implements ServerSnapshotProvider {

    public static final String PROVIDER_ID = "BATTLELOG_KEEPER";
    private static final Logger log = LoggerFactory.getLogger(BattlelogKeeperSnapshotProvider.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final KeeperSnapshotMapper mapper;

    public BattlelogKeeperSnapshotProvider(@Qualifier("keeperWebClient") WebClient webClient, KeeperSnapshotMapper mapper) {
        this.webClient = webClient;
        this.mapper = mapper;
    }

    @Override
    public Optional<ServerSnapshot> getSnapshot(ServerReference server) {
        if (server == null || server.externalGuid() == null || server.externalGuid().isBlank()) {
            return Optional.empty();
        }
        try {
            KeeperSnapshotResponse response = webClient.get()
                    .uri("/snapshot/{guid}", server.externalGuid())
                    .retrieve()
                    .bodyToMono(KeeperSnapshotResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (!isUsable(response)) {
                String status = response == null || response.snapshot() == null
                        ? "EMPTY"
                        : response.snapshot().status();
                log.warn("Snapshot Keeper indisponivel | guid={} | status={}", server.externalGuid(), status);
                return Optional.empty();
            }
            return Optional.of(mapper.map(server, response));
        } catch (WebClientResponseException exception) {
            String category = exception.getStatusCode().is5xxServerError() ? "indisponibilidade temporaria" : "requisicao rejeitada";
            log.warn("Falha HTTP no Battlelog Keeper | guid={} | status={} | category={}",
                    server.externalGuid(), exception.getStatusCode().value(), category);
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("Falha no Battlelog Keeper | guid={} | error={}", server.externalGuid(), exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean isUsable(KeeperSnapshotResponse response) {
        return response != null
                && response.snapshot() != null
                && "SUCCESS".equalsIgnoreCase(response.snapshot().status())
                && response.snapshot().currentMap() != null
                && !response.snapshot().currentMap().isBlank();
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }
}

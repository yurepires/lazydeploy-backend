package com.yurepires.lazydeploy.integration.gametools;

import com.yurepires.lazydeploy.model.server.ServerDiscoveryProvider;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

@Component
public class GameToolsServerDiscoveryProvider implements ServerDiscoveryProvider {

    public static final String PROVIDER_ID = "GAMETOOLS";
    private static final Logger log = LoggerFactory.getLogger(GameToolsServerDiscoveryProvider.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public GameToolsServerDiscoveryProvider(@Qualifier("gameToolsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public List<ServerReference> search(ServerSearchQuery query) {
        try {
            GameToolsServersResponse response = webClient.get()
                    .uri(builder -> builder.path("/bf4/servers/")
                            .queryParam("name", query.text())
                            .queryParam("limit", query.limit())
                            .queryParam("lang", "en-us")
                            .build())
                    .retrieve()
                    .bodyToMono(GameToolsServersResponse.class)
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                return List.of();
            }
            return response.servers().stream()
                    .filter(server -> externalGuid(server) != null && !externalGuid(server).isBlank())
                    .limit(query.limit())
                    .map(this::toReference)
                    .toList();
        } catch (WebClientResponseException exception) {
            log.warn("Falha HTTP no GameTools | status={}", exception.getStatusCode().value());
            return List.of();
        } catch (RuntimeException exception) {
            log.warn("Falha ao buscar servidores no GameTools: {}", exception.getMessage());
            return List.of();
        }
    }

    private ServerReference toReference(GameToolsServerResponse server) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        put(metadata, "currentMap", server.currentMap());
        put(metadata, "players", server.playerAmount());
        put(metadata, "maxPlayers", server.maxPlayers());
        put(metadata, "waitingPlayers", server.inQue());
        put(metadata, "gameMode", server.mode());
        put(metadata, "country", server.country());
        put(metadata, "region", server.region());
        put(metadata, "platform", server.platform());
        put(metadata, "serverLink", server.serverLink());
        return new ServerReference(null, externalGuid(server), server.prefix(), providerId(), metadata);
    }

    private String externalGuid(GameToolsServerResponse server) {
        return server.battlelogId() != null && !server.battlelogId().isBlank()
                ? server.battlelogId()
                : server.serverId();
    }

    private void put(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }
}

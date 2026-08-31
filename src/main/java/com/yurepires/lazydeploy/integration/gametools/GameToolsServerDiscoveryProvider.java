package com.yurepires.lazydeploy.integration.gametools;

import com.yurepires.lazydeploy.exception.ExternalProviderUnavailableException;
import com.yurepires.lazydeploy.model.server.ServerDiscoveryProvider;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSearchQuery;
import com.yurepires.lazydeploy.service.observability.ExternalProviderHealthTracker;
import com.yurepires.lazydeploy.service.observability.GameToolsMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GameToolsServerDiscoveryProvider implements ServerDiscoveryProvider {

    public static final String PROVIDER_ID = "GAMETOOLS";
    private static final Logger log = LoggerFactory.getLogger(GameToolsServerDiscoveryProvider.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final GameToolsMetrics metrics;
    private final ExternalProviderHealthTracker healthTracker;

    public GameToolsServerDiscoveryProvider(@Qualifier("gameToolsWebClient") WebClient webClient) {
        this(webClient, GameToolsMetrics.noop(), new ExternalProviderHealthTracker());
    }

    @Autowired
    public GameToolsServerDiscoveryProvider(
            @Qualifier("gameToolsWebClient") WebClient webClient,
            GameToolsMetrics metrics,
            ExternalProviderHealthTracker healthTracker
    ) {
        this.webClient = webClient;
        this.metrics = metrics;
        this.healthTracker = healthTracker;
    }

    @Override
    public List<ServerReference> search(ServerSearchQuery query) {
        long startedAt = System.nanoTime();
        String outcome = "other_error";
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
                healthTracker.recordFailure(PROVIDER_ID, "INVALID_RESPONSE");
                return List.of();
            }

            if (response.servers() == null) {
                healthTracker.recordFailure(PROVIDER_ID, "INVALID_RESPONSE");
                return List.of();
            }

            List<ServerReference> results = response.servers().stream()
                    .filter(server -> externalGuid(server) != null && !externalGuid(server).isBlank())
                    .limit(query.limit())
                    .map(this::toReference)
                    .toList();
            healthTracker.recordSuccess(PROVIDER_ID);
            metrics.recordResultCount(results.size());
            outcome = "success";
            return results;
        } catch (WebClientResponseException exception) {
            boolean serverError = exception.getStatusCode().is5xxServerError();
            String category = httpFailureCategory(serverError);
            log.warn("Falha HTTP no GameTools | status={} | category={}",
                    exception.getStatusCode().value(), category);
            healthTracker.recordFailure(PROVIDER_ID, category);
            outcome = requestOutcome(serverError);
            throw new ExternalProviderUnavailableException(
                    "O GameTools está temporariamente indisponível"
            );
        } catch (RuntimeException exception) {
            boolean timeout = isTimeout(exception);
            boolean connectionError = isConnectionError(exception);
            String category = runtimeFailureCategory(timeout, connectionError);
            log.warn("Falha ao buscar servidores no GameTools | category={}", category);
            healthTracker.recordFailure(PROVIDER_ID, category);
            outcome = runtimeOutcome(timeout);
            throw new ExternalProviderUnavailableException(
                    "Não foi possível consultar o GameTools"
            );
        } finally {
            metrics.recordRequest(outcome, Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.util.concurrent.TimeoutException
                    || current.getClass().getSimpleName().toLowerCase().contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
        if (server.battlelogId() != null && !server.battlelogId().isBlank()) {
            return server.battlelogId();
        }
        return server.serverId();
    }

    private String httpFailureCategory(boolean serverError) {
        if (serverError) {
            return "HTTP_5XX";
        }
        return "HTTP_4XX";
    }

    private String requestOutcome(boolean serverError) {
        if (serverError) {
            return "server_error";
        }
        return "client_error";
    }

    private String runtimeFailureCategory(boolean timeout, boolean connectionError) {
        if (timeout) {
            return "TIMEOUT";
        }
        if (connectionError) {
            return "CONNECTION_ERROR";
        }
        return "OTHER";
    }

    private String runtimeOutcome(boolean timeout) {
        if (timeout) {
            return "timeout";
        }
        return "other_error";
    }

    private boolean isConnectionError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WebClientRequestException
                    || current instanceof java.net.ConnectException
                    || current instanceof java.net.UnknownHostException
                    || current instanceof java.net.SocketException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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

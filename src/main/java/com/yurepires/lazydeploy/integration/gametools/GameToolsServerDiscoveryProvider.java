package com.yurepires.lazydeploy.integration.gametools;

import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.exception.ExternalProviderFailureCategory;
import com.yurepires.lazydeploy.model.server.ServerDiscoveryProvider;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSearchQuery;
import com.yurepires.lazydeploy.service.observability.ExternalProviderHealthTracker;
import com.yurepires.lazydeploy.service.observability.ExternalProviderMetrics;
import com.yurepires.lazydeploy.service.observability.GameToolsMetrics;
import com.yurepires.lazydeploy.service.provider.ExternalProviderFailureClassifier;
import com.yurepires.lazydeploy.service.provider.ProviderConcurrencyLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GameToolsServerDiscoveryProvider implements ServerDiscoveryProvider {

    public static final String PROVIDER_ID = "GAMETOOLS";
    private static final Logger log = LoggerFactory.getLogger(GameToolsServerDiscoveryProvider.class);

    private final WebClient webClient;
    private final GameToolsMetrics metrics;
    private final ExternalProviderHealthTracker healthTracker;
    private final ExternalProviderMetrics providerMetrics;
    private final ProviderConcurrencyLimiter concurrencyLimiter;
    private final ProviderProperties.ProviderSettings settings;

    public GameToolsServerDiscoveryProvider(@Qualifier("gameToolsWebClient") WebClient webClient) {
        this(
                webClient,
                GameToolsMetrics.noop(),
                new ExternalProviderHealthTracker(),
                ExternalProviderMetrics.noop(),
                null,
                ProviderProperties.ProviderSettings.gameToolsDefaults()
        );
    }

    public GameToolsServerDiscoveryProvider(
            @Qualifier("gameToolsWebClient") WebClient webClient,
            GameToolsMetrics metrics,
            ExternalProviderHealthTracker healthTracker
    ) {
        this(
                webClient,
                metrics,
                healthTracker,
                ExternalProviderMetrics.noop(),
                null,
                ProviderProperties.ProviderSettings.gameToolsDefaults()
        );
    }

    public GameToolsServerDiscoveryProvider(
            @Qualifier("gameToolsWebClient") WebClient webClient,
            GameToolsMetrics metrics,
            ExternalProviderHealthTracker healthTracker,
            ExternalProviderMetrics providerMetrics,
            ProviderConcurrencyLimiter concurrencyLimiter
    ) {
        this(
                webClient,
                metrics,
                healthTracker,
                providerMetrics,
                concurrencyLimiter,
                ProviderProperties.ProviderSettings.gameToolsDefaults()
        );
    }

    @Autowired
    public GameToolsServerDiscoveryProvider(
            @Qualifier("gameToolsWebClient") WebClient webClient,
            GameToolsMetrics metrics,
            ExternalProviderHealthTracker healthTracker,
            ExternalProviderMetrics providerMetrics,
            ProviderConcurrencyLimiter concurrencyLimiter,
            ProviderProperties providerProperties
    ) {
        this(
                webClient,
                metrics,
                healthTracker,
                providerMetrics,
                concurrencyLimiter,
                providerProperties.gameTools()
        );
    }

    private GameToolsServerDiscoveryProvider(
            WebClient webClient,
            GameToolsMetrics metrics,
            ExternalProviderHealthTracker healthTracker,
            ExternalProviderMetrics providerMetrics,
            ProviderConcurrencyLimiter concurrencyLimiter,
            ProviderProperties.ProviderSettings settings
    ) {
        this.webClient = webClient;
        this.metrics = metrics;
        this.healthTracker = healthTracker;
        this.providerMetrics = providerMetrics;
        this.concurrencyLimiter = concurrencyLimiter;
        this.settings = settings;
    }

    @Override
    public List<ServerReference> search(ServerSearchQuery query) {
        long startedAt = System.nanoTime();
        String outcome = "other_error";
        try {
            acquirePermit();
            GameToolsServersResponse response;
            try {
                response = requestServers(query);
            } finally {
                releasePermit();
            }

            if (response == null || response.servers() == null) {
                throw new ExternalProviderException(
                        PROVIDER_ID,
                        ExternalProviderFailureCategory.INVALID_RESPONSE,
                        false
                );
            }

            List<ServerReference> results = response.servers().stream()
                    .filter(this::hasExternalGuid)
                    .limit(query.limit())
                    .map(this::toReference)
                    .toList();
            healthTracker.recordSuccess(PROVIDER_ID);
            metrics.recordResultCount(results.size());
            outcome = "success";
            return results;
        } catch (ExternalProviderException exception) {
            healthTracker.recordFailure(PROVIDER_ID, exception.category().name());
            outcome = ExternalProviderFailureClassifier.outcome(exception.category());
            if (exception.category() == ExternalProviderFailureCategory.TIMEOUT) {
                providerMetrics.recordTimeout(PROVIDER_ID);
            }
            log.warn(
                    "Falha no GameTools | category={}",
                    exception.category()
            );
            throw exception;
        } catch (RuntimeException exception) {
            ExternalProviderException providerException =
                    ExternalProviderFailureClassifier.classify(PROVIDER_ID, exception);
            healthTracker.recordFailure(PROVIDER_ID, providerException.category().name());
            outcome = ExternalProviderFailureClassifier.outcome(providerException.category());
            if (providerException.category() == ExternalProviderFailureCategory.TIMEOUT) {
                providerMetrics.recordTimeout(PROVIDER_ID);
            }
            log.warn(
                    "Falha ao buscar servidores no GameTools | category={}",
                    providerException.category()
            );
            throw providerException;
        } finally {
            Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
            metrics.recordRequest(outcome, duration);
            providerMetrics.recordRequest(PROVIDER_ID, outcome, duration);
        }
    }

    private GameToolsServersResponse requestServers(ServerSearchQuery query) {
        try {
            return webClient.get()
                    .uri(builder -> builder.path("/bf4/servers/")
                            .queryParam("name", query.text())
                            .queryParam("limit", query.limit())
                            .queryParam("lang", "en-us")
                            .build())
                    .retrieve()
                    .bodyToMono(GameToolsServersResponse.class)
                    .block(Duration.ofMillis(settings.responseTimeoutMs()));
        } catch (RuntimeException exception) {
            throw ExternalProviderFailureClassifier.classify(PROVIDER_ID, exception);
        }
    }

    private void acquirePermit() {
        if (concurrencyLimiter == null) {
            return;
        }
        if (!concurrencyLimiter.tryAcquire(PROVIDER_ID)) {
            throw concurrencyLimiter.concurrencyException(PROVIDER_ID);
        }
    }

    private void releasePermit() {
        if (concurrencyLimiter != null) {
            concurrencyLimiter.release(PROVIDER_ID);
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
        if (server == null) {
            return null;
        }
        if (server.battlelogId() != null && !server.battlelogId().isBlank()) {
            return server.battlelogId();
        }
        return server.serverId();
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

    private boolean hasExternalGuid(GameToolsServerResponse server) {
        if (server == null) {
            return false;
        }
        String guid = externalGuid(server);
        return guid != null && !guid.isBlank();
    }
}

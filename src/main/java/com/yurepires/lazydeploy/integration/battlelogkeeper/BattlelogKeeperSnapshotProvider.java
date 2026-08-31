package com.yurepires.lazydeploy.integration.battlelogkeeper;

import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshotResponse;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.service.observability.ExternalProviderHealthTracker;
import com.yurepires.lazydeploy.service.observability.KeeperMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

@Component
public class BattlelogKeeperSnapshotProvider implements ServerSnapshotProvider {

    public static final String PROVIDER_ID = "BATTLELOG_KEEPER";
    private static final Logger log = LoggerFactory.getLogger(BattlelogKeeperSnapshotProvider.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final KeeperSnapshotMapper mapper;
    private final KeeperMetrics metrics;
    private final ExternalProviderHealthTracker healthTracker;

    public BattlelogKeeperSnapshotProvider(@Qualifier("keeperWebClient") WebClient webClient, KeeperSnapshotMapper mapper) {
        this(webClient, mapper, KeeperMetrics.noop(), new ExternalProviderHealthTracker());
    }

    @Autowired
    public BattlelogKeeperSnapshotProvider(
            @Qualifier("keeperWebClient") WebClient webClient,
            KeeperSnapshotMapper mapper,
            KeeperMetrics metrics,
            ExternalProviderHealthTracker healthTracker
    ) {
        this.webClient = webClient;
        this.mapper = mapper;
        this.metrics = metrics;
        this.healthTracker = healthTracker;
    }

    @Override
    public Optional<ServerSnapshot> getSnapshot(ServerReference server) {
        if (server == null || server.externalGuid() == null || server.externalGuid().isBlank()) {
            return Optional.empty();
        }

        long startedAt = System.nanoTime();
        String outcome = "other_error";
        try {
            KeeperSnapshotResponse response = webClient.get()
                    .uri("/snapshot/{guid}", server.externalGuid())
                    .retrieve()
                    .bodyToMono(KeeperSnapshotResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (!isUsable(response)) {
                String status = responseStatus(response);
                log.warn("Snapshot Keeper indisponivel | guid={} | status={}", server.externalGuid(), status);
                healthTracker.recordFailure(PROVIDER_ID, "INVALID_RESPONSE");
                outcome = "invalid_snapshot";
                return Optional.empty();
            }

            ServerSnapshot snapshot = mapper.map(server, response);
            if (snapshot == null) {
                healthTracker.recordFailure(PROVIDER_ID, "INVALID_RESPONSE");
                outcome = "invalid_snapshot";
                return Optional.empty();
            }

            healthTracker.recordSuccess(PROVIDER_ID);
            outcome = "success";
            return Optional.of(snapshot);
        } catch (WebClientResponseException exception) {
            boolean serverError = exception.getStatusCode().is5xxServerError();
            String category = httpFailureCategory(serverError);
            log.warn("Falha HTTP no Battlelog Keeper | guid={} | status={} | category={}",
                    server.externalGuid(), exception.getStatusCode().value(), category);
            healthTracker.recordFailure(PROVIDER_ID, category);
            outcome = requestOutcome(serverError);
            return Optional.empty();
        } catch (RuntimeException exception) {
            boolean timeout = isTimeout(exception);
            boolean connectionError = isConnectionError(exception);
            String category = runtimeFailureCategory(timeout, connectionError);
            log.warn("Falha no Battlelog Keeper | guid={} | category={}", server.externalGuid(), category);
            healthTracker.recordFailure(PROVIDER_ID, category);
            outcome = runtimeOutcome(timeout);
            return Optional.empty();
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

    private boolean isUsable(KeeperSnapshotResponse response) {
        return response != null
                && response.snapshot() != null
                && "SUCCESS".equalsIgnoreCase(response.snapshot().status())
                && response.snapshot().currentMap() != null
                && !response.snapshot().currentMap().isBlank();
    }

    private String responseStatus(KeeperSnapshotResponse response) {
        if (response == null || response.snapshot() == null) {
            return "EMPTY";
        }
        if (response.snapshot().status() == null || response.snapshot().status().isBlank()) {
            return "EMPTY";
        }
        return response.snapshot().status();
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

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }
}

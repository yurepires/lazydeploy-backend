package com.yurepires.lazydeploy.integration.battlelogkeeper;

import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshotResponse;
import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.exception.ExternalProviderFailureCategory;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.service.observability.ExternalProviderHealthTracker;
import com.yurepires.lazydeploy.service.observability.ExternalProviderMetrics;
import com.yurepires.lazydeploy.service.observability.KeeperMetrics;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import com.yurepires.lazydeploy.service.observability.SecurityEventLogger;
import com.yurepires.lazydeploy.service.observability.SecurityEventOutcome;
import com.yurepires.lazydeploy.service.observability.SecurityEventType;
import com.yurepires.lazydeploy.service.provider.ExternalProviderFailureClassifier;
import com.yurepires.lazydeploy.service.provider.ProviderConcurrencyLimiter;
import com.yurepires.lazydeploy.service.provider.ProviderRetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

@Component
public class BattlelogKeeperSnapshotProvider implements ServerSnapshotProvider {

    public static final String PROVIDER_ID = "BATTLELOG_KEEPER";
    private static final Logger log = LoggerFactory.getLogger(BattlelogKeeperSnapshotProvider.class);

    private final WebClient webClient;
    private final KeeperSnapshotMapper mapper;
    private final KeeperSnapshotValidator validator;
    private final KeeperMetrics metrics;
    private final ExternalProviderHealthTracker healthTracker;
    private final ExternalProviderMetrics providerMetrics;
    private final ProviderConcurrencyLimiter concurrencyLimiter;
    private final ProviderRetryExecutor retryExecutor;
    private final ProviderProperties.ProviderSettings settings;
    private final SecurityEventLogger securityEventLogger;

    public BattlelogKeeperSnapshotProvider(@Qualifier("keeperWebClient") WebClient webClient, KeeperSnapshotMapper mapper) {
        this(
                webClient,
                mapper,
                new KeeperSnapshotValidator(new MapIdentifierNormalizer()),
                KeeperMetrics.noop(),
                new ExternalProviderHealthTracker(),
                ExternalProviderMetrics.noop(),
                null,
                new ProviderRetryExecutor(ExternalProviderMetrics.noop()),
                ProviderProperties.ProviderSettings.keeperDefaults(),
                new SecurityEventLogger(new LogSanitizer())
        );
    }

    public BattlelogKeeperSnapshotProvider(
            @Qualifier("keeperWebClient") WebClient webClient,
            KeeperSnapshotMapper mapper,
            KeeperMetrics metrics,
            ExternalProviderHealthTracker healthTracker
    ) {
        this(
                webClient,
                mapper,
                new KeeperSnapshotValidator(new MapIdentifierNormalizer()),
                metrics,
                healthTracker,
                ExternalProviderMetrics.noop(),
                null,
                new ProviderRetryExecutor(ExternalProviderMetrics.noop()),
                ProviderProperties.ProviderSettings.keeperDefaults(),
                new SecurityEventLogger(new LogSanitizer())
        );
    }

    @Autowired
    public BattlelogKeeperSnapshotProvider(
            @Qualifier("keeperWebClient") WebClient webClient,
            KeeperSnapshotMapper mapper,
            KeeperSnapshotValidator validator,
            KeeperMetrics metrics,
            ExternalProviderHealthTracker healthTracker,
            ExternalProviderMetrics providerMetrics,
            ProviderConcurrencyLimiter concurrencyLimiter,
            ProviderRetryExecutor retryExecutor,
            ProviderProperties providerProperties,
            SecurityEventLogger securityEventLogger
    ) {
        this(
                webClient,
                mapper,
                validator,
                metrics,
                healthTracker,
                providerMetrics,
                concurrencyLimiter,
                retryExecutor,
                providerProperties.keeper(),
                securityEventLogger
        );
    }

    private BattlelogKeeperSnapshotProvider(
            WebClient webClient,
            KeeperSnapshotMapper mapper,
            KeeperSnapshotValidator validator,
            KeeperMetrics metrics,
            ExternalProviderHealthTracker healthTracker,
            ExternalProviderMetrics providerMetrics,
            ProviderConcurrencyLimiter concurrencyLimiter,
            ProviderRetryExecutor retryExecutor,
            ProviderProperties.ProviderSettings settings,
            SecurityEventLogger securityEventLogger
    ) {
        this.webClient = webClient;
        this.mapper = mapper;
        this.validator = validator;
        this.metrics = metrics;
        this.healthTracker = healthTracker;
        this.providerMetrics = providerMetrics;
        this.concurrencyLimiter = concurrencyLimiter;
        this.retryExecutor = retryExecutor;
        this.settings = settings;
        this.securityEventLogger = securityEventLogger;
    }

    @Override
    public Optional<ServerSnapshot> getSnapshot(ServerReference server) {
        if (server == null || server.externalGuid() == null || server.externalGuid().isBlank()) {
            return Optional.empty();
        }

        long startedAt = System.nanoTime();
        String outcome = "other_error";
        try {
            ServerSnapshot snapshot = executeWithRetry(server);
            healthTracker.recordSuccess(PROVIDER_ID);
            outcome = "success";
            return Optional.of(snapshot);
        } catch (ExternalProviderException exception) {
            healthTracker.recordFailure(PROVIDER_ID, exception.category().name());
            outcome = ExternalProviderFailureClassifier.outcome(exception.category());
            if (retryExecutor == null
                    && exception.category() == ExternalProviderFailureCategory.TIMEOUT) {
                providerMetrics.recordTimeout(PROVIDER_ID);
            }
            logProviderFailure(exception.category(), exception);
            return Optional.empty();
        } catch (RuntimeException exception) {
            ExternalProviderException providerException =
                    ExternalProviderFailureClassifier.classify(PROVIDER_ID, exception);
            healthTracker.recordFailure(PROVIDER_ID, providerException.category().name());
            outcome = ExternalProviderFailureClassifier.outcome(providerException.category());
            if (retryExecutor == null
                    && providerException.category() == ExternalProviderFailureCategory.TIMEOUT) {
                providerMetrics.recordTimeout(PROVIDER_ID);
            }
            logProviderFailure(providerException.category(), providerException);
            return Optional.empty();
        } finally {
            Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
            metrics.recordRequest(outcome, duration);
            providerMetrics.recordRequest(PROVIDER_ID, outcome, duration);
        }
    }

    private void logProviderFailure(
            ExternalProviderFailureCategory category,
            Throwable failure
    ) {
        if (category == ExternalProviderFailureCategory.UNKNOWN) {
            log.error(
                    "PROVIDER FAILURE | provider=keeper | category={}",
                    category,
                    failure
            );
        } else {
            log.warn("PROVIDER FAILURE | provider=keeper | category={}", category);
        }
        securityEventLogger.log(
                SecurityEventType.PROVIDER_FAILURE,
                SecurityEventOutcome.FAILURE,
                category == null ? "UNKNOWN" : category.name(),
                "/external/keeper"
        );
    }

    private ServerSnapshot executeWithRetry(ServerReference server) {
        if (retryExecutor == null) {
            return requestSnapshot(server);
        }

        return retryExecutor.execute(
                PROVIDER_ID,
                settings.retry(),
                () -> requestSnapshot(server)
        );
    }

    private ServerSnapshot requestSnapshot(ServerReference server) {
        acquirePermit();
        try {
            KeeperSnapshotResponse response = webClient.get()
                    .uri("/snapshot/{guid}", server.externalGuid())
                    .retrieve()
                    .bodyToMono(KeeperSnapshotResponse.class)
                    .block(Duration.ofMillis(settings.responseTimeoutMs()));

            if (!validator.isValid(response)) {
                throw new ExternalProviderException(
                        PROVIDER_ID,
                        ExternalProviderFailureCategory.INVALID_RESPONSE,
                        false
                );
            }

            ServerSnapshot snapshot = mapper.map(server, response);
            if (snapshot == null) {
                throw new ExternalProviderException(
                        PROVIDER_ID,
                        ExternalProviderFailureCategory.INVALID_RESPONSE,
                        false
                );
            }
            return snapshot;
        } catch (ExternalProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ExternalProviderFailureClassifier.classify(PROVIDER_ID, exception);
        } finally {
            releasePermit();
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

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }
}

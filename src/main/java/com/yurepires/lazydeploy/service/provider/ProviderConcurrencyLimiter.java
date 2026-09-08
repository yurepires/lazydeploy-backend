package com.yurepires.lazydeploy.service.provider;

import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.exception.ExternalProviderFailureCategory;
import com.yurepires.lazydeploy.service.observability.ExternalProviderMetrics;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import com.yurepires.lazydeploy.service.observability.SecurityEventLogger;
import com.yurepires.lazydeploy.service.observability.SecurityEventOutcome;
import com.yurepires.lazydeploy.service.observability.SecurityEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Bulkhead simples por provider. A aquisição é fail-fast para não criar fila
 * de threads aguardando uma integração lenta.
 */
@Component
public class ProviderConcurrencyLimiter {

    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final ExternalProviderMetrics metrics;
    private final SecurityEventLogger securityEventLogger;

    public ProviderConcurrencyLimiter(
            ProviderProperties properties,
            ExternalProviderMetrics metrics
    ) {
        this(properties, metrics, new SecurityEventLogger(new LogSanitizer()));
    }

    @Autowired
    public ProviderConcurrencyLimiter(
            ProviderProperties properties,
            ExternalProviderMetrics metrics,
            SecurityEventLogger securityEventLogger
    ) {
        this.metrics = metrics;
        this.securityEventLogger = securityEventLogger;
        semaphores.put("GAMETOOLS", new Semaphore(properties.gameTools().maxConcurrentRequests()));
        semaphores.put("BATTLELOG_KEEPER", new Semaphore(properties.keeper().maxConcurrentRequests()));
        semaphores.put("BFLIST", new Semaphore(properties.bflist().maxConcurrentRequests()));
    }

    public boolean tryAcquire(String providerId) {
        Semaphore semaphore = semaphoreFor(providerId);
        boolean acquired = semaphore.tryAcquire();
        if (!acquired) {
            metrics.recordConcurrencyRejection(providerId);
            securityEventLogger.log(
                    SecurityEventType.RESOURCE_SATURATION,
                    SecurityEventOutcome.REJECTED,
                    "PROVIDER_BULKHEAD",
                    "/external/provider"
            );
        }
        return acquired;
    }

    public void release(String providerId) {
        semaphoreFor(providerId).release();
    }

    public ExternalProviderException concurrencyException(String providerId) {
        return new ExternalProviderException(
                providerId,
                ExternalProviderFailureCategory.CONCURRENCY_LIMIT_REACHED,
                false
        );
    }

    private Semaphore semaphoreFor(String providerId) {
        if (providerId == null) {
            return semaphores.get("BFLIST");
        }

        Semaphore semaphore = semaphores.get(providerId.trim().toUpperCase(Locale.ROOT));
        if (semaphore == null) {
            return semaphores.get("BFLIST");
        }
        return semaphore;
    }
}

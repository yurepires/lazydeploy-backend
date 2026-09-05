package com.yurepires.lazydeploy.service.provider;

import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.exception.ExternalProviderFailureCategory;
import com.yurepires.lazydeploy.service.observability.ExternalProviderMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/** Executa no máximo uma tentativa inicial e um retry transitório. */
@Component
public class ProviderRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProviderRetryExecutor.class);

    private final ExternalProviderMetrics metrics;

    public ProviderRetryExecutor(ExternalProviderMetrics metrics) {
        this.metrics = metrics;
    }

    public <T> T execute(
            String providerId,
            ProviderProperties.RetrySettings retrySettings,
            Supplier<T> operation
    ) {
        int maximumAttempts = maximumAttempts(retrySettings);

        for (int attempt = 1; attempt <= maximumAttempts; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException failure) {
                ExternalProviderException providerException =
                        ExternalProviderFailureClassifier.classify(providerId, failure);

                if (providerException.category() == ExternalProviderFailureCategory.TIMEOUT) {
                    metrics.recordTimeout(providerId);
                }

                if (!shouldRetry(providerException, attempt, maximumAttempts)) {
                    throw providerException;
                }

                log.debug(
                        "Retry de provider externo | provider={} | attempt={} | category={}",
                        providerId,
                        attempt + 1,
                        providerException.category()
                );
                metrics.recordRetry(
                        providerId,
                        ExternalProviderFailureClassifier.outcome(providerException.category())
                );
                waitBeforeRetry(providerId, retrySettings.backoffMs());
            }
        }

        throw new ExternalProviderException(
                providerId,
                ExternalProviderFailureCategory.UNKNOWN,
                false
        );
    }

    private int maximumAttempts(ProviderProperties.RetrySettings retrySettings) {
        if (retrySettings == null || !retrySettings.enabled()) {
            return 1;
        }

        return Math.min(retrySettings.maxAttempts(), 2);
    }

    private boolean shouldRetry(
            ExternalProviderException exception,
            int currentAttempt,
            int maximumAttempts
    ) {
        return exception.retryable() && currentAttempt < maximumAttempts;
    }

    private void waitBeforeRetry(String providerId, long backoffMs) {
        if (backoffMs <= 0) {
            return;
        }

        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ExternalProviderException(
                    providerId,
                    ExternalProviderFailureCategory.UNKNOWN,
                    false,
                    interruptedException
            );
        }
    }
}

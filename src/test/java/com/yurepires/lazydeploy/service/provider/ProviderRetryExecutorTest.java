package com.yurepires.lazydeploy.service.provider;

import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.exception.ExternalProviderFailureCategory;
import com.yurepires.lazydeploy.service.observability.ExternalProviderMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderRetryExecutorTest {

    @Test
    void shouldRetryOnceForTimeoutAndReturnRecoveredResult() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(
                new ExternalProviderMetrics(meterRegistry)
        );
        ProviderProperties.RetrySettings retrySettings =
                new ProviderProperties.RetrySettings(true, 2, 0);
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(
                "BATTLELOG_KEEPER",
                retrySettings,
                () -> {
                    if (attempts.getAndIncrement() == 0) {
                        throw new RuntimeException(new TimeoutException("timeout"));
                    }
                    return "recovered";
                }
        );

        assertThat(result).isEqualTo("recovered");
        assertThat(attempts).hasValue(2);
        assertThat(meterRegistry.get("lazydeploy.provider.retries")
                .tag("provider", "keeper")
                .tag("outcome", "timeout")
                .counter()
                .count()).isEqualTo(1);
    }

    @Test
    void shouldNotRetryClientErrors() {
        ProviderRetryExecutor executor = new ProviderRetryExecutor(
                ExternalProviderMetrics.noop()
        );
        ProviderProperties.RetrySettings retrySettings =
                new ProviderProperties.RetrySettings(true, 2, 0);
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute(
                "BATTLELOG_KEEPER",
                retrySettings,
                () -> {
                    attempts.incrementAndGet();
                    throw new ExternalProviderException(
                            "BATTLELOG_KEEPER",
                            ExternalProviderFailureCategory.CLIENT_ERROR,
                            false
                    );
                }
        )).isInstanceOfSatisfying(
                ExternalProviderException.class,
                exception -> assertThat(exception.category())
                        .isEqualTo(ExternalProviderFailureCategory.CLIENT_ERROR)
        );

        assertThat(attempts).hasValue(1);
    }
}

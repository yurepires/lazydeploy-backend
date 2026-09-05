package com.yurepires.lazydeploy.service.provider;

import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.service.observability.ExternalProviderMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderConcurrencyLimiterTest {

    @Test
    void shouldRejectWhenProviderLimitIsReachedAndReleasePermitAfterward() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ProviderProperties.ProviderSettings singleRequest =
                new ProviderProperties.ProviderSettings(
                        2_000,
                        5_000,
                        1,
                        new ProviderProperties.RetrySettings(false, 1, 0)
                );
        ProviderProperties properties = new ProviderProperties(
                singleRequest,
                singleRequest,
                singleRequest
        );
        ProviderConcurrencyLimiter limiter = new ProviderConcurrencyLimiter(
                properties,
                new ExternalProviderMetrics(meterRegistry)
        );

        assertThat(limiter.tryAcquire("GAMETOOLS")).isTrue();
        assertThat(limiter.tryAcquire("GAMETOOLS")).isFalse();
        assertThat(meterRegistry.get("lazydeploy.provider.concurrency_rejections")
                .tag("provider", "gametools")
                .counter()
                .count()).isEqualTo(1);

        limiter.release("GAMETOOLS");

        assertThat(limiter.tryAcquire("GAMETOOLS")).isTrue();
        limiter.release("GAMETOOLS");
    }
}

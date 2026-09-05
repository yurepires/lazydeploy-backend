package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.config.RateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Test
    void shouldBlockAfterConfiguredCapacityIsConsumed() {
        RateLimitProperties properties = propertiesWithCache();
        RateLimitService service = new RateLimitService(properties);
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy(
                2,
                Duration.ofMinutes(1)
        );

        assertThat(service.tryConsume("login_ip", "127.0.0.1", policy).allowed())
                .isTrue();
        assertThat(service.tryConsume("login_ip", "127.0.0.1", policy).allowed())
                .isTrue();

        RateLimitDecision blocked = service.tryConsume(
                "login_ip",
                "127.0.0.1",
                policy
        );

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldKeepBucketsIndependentByPolicyAndKey() {
        RateLimitProperties properties = propertiesWithCache();
        RateLimitService service = new RateLimitService(properties);
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy(
                1,
                Duration.ofMinutes(1)
        );

        assertThat(service.tryConsume("login_ip", "first", policy).allowed())
                .isTrue();
        assertThat(service.tryConsume("login_ip", "first", policy).allowed())
                .isFalse();
        assertThat(service.tryConsume("login_ip", "second", policy).allowed())
                .isTrue();
        assertThat(service.tryConsume("login_email", "first", policy).allowed())
                .isTrue();
    }

    @Test
    void shouldRejectCacheExpirationShorterThanPolicyWindow() {
        assertThatThrownBy(() -> new RateLimitProperties(
                true,
                RateLimitProperties.LoginPolicy.defaults(),
                new RateLimitProperties.Policy(5, Duration.ofMinutes(10)),
                new RateLimitProperties.EndpointPolicy(
                        new RateLimitProperties.Policy(30, Duration.ofMinutes(1)),
                        new RateLimitProperties.Policy(60, Duration.ofMinutes(1))
                ),
                new RateLimitProperties.EndpointPolicy(
                        new RateLimitProperties.Policy(10, Duration.ofMinutes(10)),
                        new RateLimitProperties.Policy(20, Duration.ofMinutes(10))
                ),
                new RateLimitProperties.CacheProperties(100, Duration.ofMinutes(1)),
                new RateLimitProperties.ProxyPolicy(false, List.of())
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiração do cache");
    }

    private RateLimitProperties propertiesWithCache() {
        return new RateLimitProperties(
                true,
                RateLimitProperties.LoginPolicy.defaults(),
                new RateLimitProperties.Policy(5, Duration.ofMinutes(10)),
                new RateLimitProperties.EndpointPolicy(
                        new RateLimitProperties.Policy(30, Duration.ofMinutes(1)),
                        new RateLimitProperties.Policy(60, Duration.ofMinutes(1))
                ),
                new RateLimitProperties.EndpointPolicy(
                        new RateLimitProperties.Policy(10, Duration.ofMinutes(10)),
                        new RateLimitProperties.Policy(20, Duration.ofMinutes(10))
                ),
                new RateLimitProperties.CacheProperties(100, Duration.ofMinutes(30)),
                new RateLimitProperties.ProxyPolicy(false, List.of())
        );
    }
}

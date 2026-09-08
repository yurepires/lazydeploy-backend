package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityMetricsTest {

    @Test
    void shouldRecordRateLimitOutcomesWithoutPersonalIdentifiersInTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SecurityMetrics metrics = new SecurityMetrics(registry);

        metrics.recordRateLimitRequest("login_ip", "allowed");
        metrics.recordRateLimitRequest("login_ip", "blocked");
        metrics.recordLoginAttempt("rate_limited");

        assertThat(registry.get(SecurityMetrics.RATE_LIMIT_REQUESTS)
                .tag("policy", "login_ip")
                .tag("outcome", "allowed")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get(SecurityMetrics.RATE_LIMIT_REQUESTS)
                .tag("policy", "login_ip")
                .tag("outcome", "blocked")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get(SecurityMetrics.LOGIN_ATTEMPTS)
                .tag("outcome", "rate_limited")
                .counter()
                .count()).isEqualTo(1);

        metrics.recordHttpResponse(401, "authentication");
        metrics.recordHttpResponse(403, "csrf");
        metrics.recordHttpResponse(429, "rate_limit");
        metrics.recordCsrfRejection("/api/auth/login");
        metrics.recordAuthorizationRejection("not_owned");

        assertThat(registry.get(SecurityMetrics.HTTP_RESPONSES)
                .tag("status_class", "4xx")
                .tag("security_reason", "authentication")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get(SecurityMetrics.CSRF_REJECTIONS)
                .tag("endpoint_group", "auth")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get(SecurityMetrics.AUTHORIZATION_REJECTIONS)
                .tag("reason", "not_owned")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allMatch(meter -> meter.getId().getTags().stream()
                        .noneMatch(tag -> tag.getKey().equals("ip")
                                || tag.getKey().equals("email")
                                || tag.getKey().equals("userId")
                                || tag.getKey().equals("subscriptionId")));
    }
}

package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.config.RateLimitProperties;
import com.yurepires.lazydeploy.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver(
            new ClientIpResolver(properties())
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNormalizeLoginEmailUsingAuthenticationPolicy() {
        assertThat(resolver.normalizeLoginEmail("  User@Example.COM "))
                .isEqualTo("user@example.com");
        assertThat(resolver.normalizeLoginEmail("  ")).isNull();
    }

    @Test
    void shouldResolveAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "user@example.com",
                "{bcrypt}hash",
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        assertThat(resolver.resolveAuthenticatedUser()).isEqualTo(userId.toString());
    }

    @Test
    void shouldResolveClientIpThroughConfiguredResolver() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("192.0.2.20");
    }

    private RateLimitProperties properties() {
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

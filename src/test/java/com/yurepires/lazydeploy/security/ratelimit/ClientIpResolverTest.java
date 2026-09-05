package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void shouldIgnoreForwardedHeadersByDefault() {
        ClientIpResolver resolver = new ClientIpResolver(properties(false, List.of()));
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader("X-Forwarded-For", "192.0.2.10");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void shouldUseForwardedClientOnlyWhenRemoteProxyIsTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(
                properties(true, List.of("10.0.0.5"))
        );
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader("X-Forwarded-For", "192.0.2.10, 10.0.0.4");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.4");
    }

    @Test
    void shouldNotTrustForwardedHeadersFromAnUnconfiguredProxy() {
        ClientIpResolver resolver = new ClientIpResolver(
                properties(true, List.of("10.0.0.5"))
        );
        MockHttpServletRequest request = request("10.0.0.6");
        request.addHeader("X-Forwarded-For", "192.0.2.10");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.6");
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private RateLimitProperties properties(
            boolean trustForwardedHeaders,
            List<String> trustedProxies
    ) {
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
                new RateLimitProperties.ProxyPolicy(trustForwardedHeaders, trustedProxies)
        );
    }
}

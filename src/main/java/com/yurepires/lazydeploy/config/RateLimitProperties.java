package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Limites de requisições aplicados antes dos controllers.
 *
 * <p>Os limites são mantidos em memória porque esta versão da aplicação roda
 * em uma única instância. Em um ambiente com várias instâncias, o armazenamento
 * deverá ser substituído por um backend compartilhado.</p>
 */
@ConfigurationProperties(prefix = "lazydeploy.security.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        LoginPolicy login,
        Policy register,
        EndpointPolicy serverSearch,
        EndpointPolicy subscriptionConfigure,
        CacheProperties cache,
        ProxyPolicy proxy
) {

    public RateLimitProperties {
        login = valueOrDefault(login, LoginPolicy.defaults());
        register = valueOrDefault(register, new Policy(5, Duration.ofMinutes(10)));
        serverSearch = valueOrDefault(
                serverSearch,
                new EndpointPolicy(
                        new Policy(30, Duration.ofMinutes(1)),
                        new Policy(60, Duration.ofMinutes(1))
                )
        );
        subscriptionConfigure = valueOrDefault(
                subscriptionConfigure,
                new EndpointPolicy(
                        new Policy(10, Duration.ofMinutes(10)),
                        new Policy(20, Duration.ofMinutes(10))
                )
        );
        cache = valueOrDefault(
                cache,
                new CacheProperties(10_000, Duration.ofMinutes(30))
        );
        proxy = valueOrDefault(proxy, ProxyPolicy.defaults());

        Duration largestPolicyWindow = largestPolicyWindow(
                login,
                register,
                serverSearch,
                subscriptionConfigure
        );
        if (cache.expireAfterAccess().compareTo(largestPolicyWindow) < 0) {
            throw new IllegalArgumentException(
                    "A expiração do cache deve ser igual ou maior que a maior janela de rate limit"
            );
        }
    }

    private static <T> T valueOrDefault(T value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private static Duration largestPolicyWindow(
            LoginPolicy login,
            Policy register,
            EndpointPolicy serverSearch,
            EndpointPolicy subscriptionConfigure
    ) {
        Duration largestWindow = login.ip().window();
        largestWindow = maximum(largestWindow, login.identity().window());
        largestWindow = maximum(largestWindow, register.window());
        largestWindow = maximum(largestWindow, serverSearch.user().window());
        largestWindow = maximum(largestWindow, serverSearch.ip().window());
        largestWindow = maximum(largestWindow, subscriptionConfigure.user().window());
        return maximum(largestWindow, subscriptionConfigure.ip().window());
    }

    private static Duration maximum(Duration first, Duration second) {
        if (first.compareTo(second) >= 0) {
            return first;
        }
        return second;
    }

    public record LoginPolicy(
            Policy ip,
            Policy identity
    ) {

        public LoginPolicy {
            ip = valueOrDefault(ip, new Policy(10, Duration.ofMinutes(1)));
            identity = valueOrDefault(identity, new Policy(5, Duration.ofMinutes(5)));
        }

        public static LoginPolicy defaults() {
            return new LoginPolicy(
                    new Policy(10, Duration.ofMinutes(1)),
                    new Policy(5, Duration.ofMinutes(5))
            );
        }
    }

    public record EndpointPolicy(
            Policy user,
            Policy ip
    ) {

        public EndpointPolicy {
            user = valueOrDefault(user, new Policy(1, Duration.ofMinutes(1)));
            ip = valueOrDefault(ip, new Policy(1, Duration.ofMinutes(1)));
        }
    }

    public record Policy(
            long capacity,
            Duration window
    ) {

        public Policy {
            if (capacity < 1) {
                throw new IllegalArgumentException("A capacidade do rate limit deve ser positiva");
            }
            if (window == null || window.isZero() || window.isNegative()) {
                throw new IllegalArgumentException("A janela do rate limit deve ser positiva");
            }
        }
    }

    public record CacheProperties(
            long maximumSize,
            Duration expireAfterAccess
    ) {

        public CacheProperties {
            if (maximumSize < 1) {
                throw new IllegalArgumentException("O tamanho máximo do cache deve ser positivo");
            }
            if (expireAfterAccess == null
                    || expireAfterAccess.isZero()
                    || expireAfterAccess.isNegative()) {
                throw new IllegalArgumentException("A expiração do cache deve ser positiva");
            }
        }
    }

    public record ProxyPolicy(
            boolean trustForwardedHeaders,
            List<String> trustedProxies
    ) {

        public ProxyPolicy {
            if (trustedProxies == null) {
                trustedProxies = List.of();
            } else {
                trustedProxies = trustedProxies.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();
            }
        }

        public static ProxyPolicy defaults() {
            return new ProxyPolicy(false, List.of());
        }
    }
}

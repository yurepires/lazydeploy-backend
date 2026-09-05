package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Métricas agregadas da proteção de autenticação e de rate limiting.
 *
 * <p>As tags são deliberadamente limitadas a valores de política e resultado.
 * Endereços IP, e-mails, usuários e subscriptions nunca são tags.</p>
 */
@Component
public class SecurityMetrics {

    public static final String RATE_LIMIT_REQUESTS =
            "lazydeploy.security.rate_limit.requests";
    public static final String LOGIN_ATTEMPTS =
            "lazydeploy.security.login.attempts";

    private static final Set<String> RATE_LIMIT_OUTCOMES = Set.of("allowed", "blocked");
    private static final Set<String> LOGIN_OUTCOMES = Set.of(
            "success",
            "invalid_credentials",
            "rate_limited"
    );

    private final MeterRegistry meterRegistry;

    public SecurityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRateLimitRequest(String policy, String outcome) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(RATE_LIMIT_REQUESTS)
                    .tag("policy", safePolicy(policy))
                    .tag("outcome", normalize(outcome, RATE_LIMIT_OUTCOMES, "blocked"))
                    .description("Requisições avaliadas pelo rate limiting")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Observabilidade nunca deve interromper uma requisição.
        }
    }

    public void recordLoginAttempt(String outcome) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(LOGIN_ATTEMPTS)
                    .tag("outcome", normalize(outcome, LOGIN_OUTCOMES, "invalid_credentials"))
                    .description("Tentativas de autenticação local")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Observabilidade nunca deve interromper uma requisição.
        }
    }

    private String safePolicy(String policy) {
        if (policy == null || policy.isBlank()) {
            return "unknown";
        }
        return policy.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize(String value, Set<String> allowedValues, String fallback) {
        if (value == null) {
            return fallback;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        if (allowedValues.contains(normalizedValue)) {
            return normalizedValue;
        }
        return fallback;
    }
}

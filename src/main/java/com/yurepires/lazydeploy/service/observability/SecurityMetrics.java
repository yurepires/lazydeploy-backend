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
    public static final String HTTP_RESPONSES =
            "lazydeploy.security.http.responses";
    public static final String CSRF_REJECTIONS =
            "lazydeploy.security.csrf.rejections";
    public static final String AUTHENTICATION_ATTEMPTS =
            "lazydeploy.security.authentication.attempts";
    public static final String AUTHORIZATION_REJECTIONS =
            "lazydeploy.security.authorization.rejections";

    private static final Set<String> RATE_LIMIT_OUTCOMES = Set.of("allowed", "blocked");
    private static final Set<String> RATE_LIMIT_POLICIES = Set.of(
            "login_ip",
            "login_identity",
            "register_ip",
            "server_search_user",
            "server_search_ip",
            "configure_user",
            "configure_ip",
            "subscription_configure_user",
            "subscription_configure_ip"
    );
    private static final Set<String> LOGIN_OUTCOMES = Set.of(
            "success",
            "invalid_credentials",
            "rate_limited"
    );
    private static final Set<String> AUTHORIZATION_REASONS = Set.of(
            "unauthenticated",
            "forbidden",
            "not_owned"
    );
    private static final Set<String> SECURITY_REASONS = Set.of(
            "authentication",
            "authorization",
            "csrf",
            "rate_limit",
            "business_limit",
            "validation",
            "not_found",
            "other"
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
        recordAuthenticationAttempt(outcome);
        recordLegacyLoginAttempt(outcome);
    }

    public void recordAuthenticationAttempt(String outcome) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(AUTHENTICATION_ATTEMPTS)
                    .tag("outcome", normalize(outcome, LOGIN_OUTCOMES, "invalid_credentials"))
                    .description("Tentativas de autenticação")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Observabilidade nunca deve interromper uma requisição.
        }
    }

    public void recordHttpResponse(int status, String securityReason) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(HTTP_RESPONSES)
                    .tag("status_class", statusClass(status))
                    .tag("security_reason", normalize(
                            securityReason,
                            SECURITY_REASONS,
                            "other"
                    ))
                    .description("Respostas HTTP relacionadas à segurança")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Observabilidade nunca deve interromper uma resposta HTTP.
        }
    }

    public void recordCsrfRejection(String endpointGroup) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(CSRF_REJECTIONS)
                    .tag("endpoint_group", normalizeEndpointGroup(endpointGroup))
                    .description("Rejeições por falha de CSRF")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Observabilidade nunca deve interromper uma resposta HTTP.
        }
    }

    public void recordAuthorizationRejection(String reason) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(AUTHORIZATION_REJECTIONS)
                    .tag("reason", normalize(reason, AUTHORIZATION_REASONS, "forbidden"))
                    .description("Rejeições de autenticação e autorização")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Observabilidade nunca deve interromper uma resposta HTTP.
        }
    }

    private void recordLegacyLoginAttempt(String outcome) {
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
            // Mantém compatibilidade com a métrica legada sem afetar a aplicação.
        }
    }

    private String statusClass(int status) {
        int hundreds = status / 100;
        if (hundreds == 2) {
            return "2xx";
        }
        if (hundreds == 4) {
            return "4xx";
        }
        if (hundreds == 5) {
            return "5xx";
        }
        return "other";
    }

    private String normalizeEndpointGroup(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "unknown";
        }

        String normalizedEndpoint = endpoint.trim().toLowerCase(Locale.ROOT);
        if (normalizedEndpoint.startsWith("/api/auth/")) {
            return "auth";
        }
        if (normalizedEndpoint.startsWith("/api/bf4/")) {
            return "bf4";
        }
        return "other";
    }

    private String safePolicy(String policy) {
        if (policy == null || policy.isBlank()) {
            return "unknown";
        }
        String normalizedPolicy = policy.trim().toLowerCase(Locale.ROOT);
        if ("subscription_configure_user".equals(normalizedPolicy)) {
            return "configure_user";
        }
        if ("subscription_configure_ip".equals(normalizedPolicy)) {
            return "configure_ip";
        }
        if (RATE_LIMIT_POLICIES.contains(normalizedPolicy)) {
            return normalizedPolicy;
        }
        return "unknown";
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

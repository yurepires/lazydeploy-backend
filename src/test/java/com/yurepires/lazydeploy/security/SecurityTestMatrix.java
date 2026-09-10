package com.yurepires.lazydeploy.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matriz central que documenta as proteções esperadas para os endpoints
 * sensíveis. Os testes de contrato podem consultar esta matriz para manter a
 * intenção de segurança próxima da suíte automatizada.
 */
public final class SecurityTestMatrix {

    public enum Protection {
        AUTHENTICATION,
        AUTHORIZATION,
        OWNERSHIP,
        CSRF,
        IP_RATE_LIMIT,
        USER_RATE_LIMIT,
        IDENTITY_RATE_LIMIT,
        PAYLOAD_LIMIT,
        PAGINATION_LIMIT,
        QUERY_VALIDATION,
        GENERIC_ERRORS,
        SESSION_FIXATION,
        SESSION_INVALIDATION,
        CORS,
        SECURITY_HEADERS,
        PROVIDER_TIMEOUT,
        PROVIDER_BULKHEAD
    }

    private SecurityTestMatrix() {
    }

    public static Map<String, Set<Protection>> expectedProtections() {
        Map<String, Set<Protection>> matrix = new LinkedHashMap<>();

        add(matrix, "POST /api/auth/register",
                Protection.CSRF,
                Protection.IP_RATE_LIMIT,
                Protection.PAYLOAD_LIMIT,
                Protection.GENERIC_ERRORS);
        add(matrix, "POST /api/auth/login",
                Protection.CSRF,
                Protection.IP_RATE_LIMIT,
                Protection.IDENTITY_RATE_LIMIT,
                Protection.PAYLOAD_LIMIT,
                Protection.GENERIC_ERRORS,
                Protection.SESSION_FIXATION);
        add(matrix, "POST /api/auth/logout",
                Protection.AUTHENTICATION,
                Protection.CSRF,
                Protection.SESSION_INVALIDATION);
        add(matrix, "GET /api/auth/me",
                Protection.AUTHENTICATION,
                Protection.GENERIC_ERRORS);
        add(matrix, "GET /api/bf4/servers/search",
                Protection.AUTHENTICATION,
                Protection.QUERY_VALIDATION,
                Protection.USER_RATE_LIMIT,
                Protection.IP_RATE_LIMIT,
                Protection.PROVIDER_TIMEOUT,
                Protection.PROVIDER_BULKHEAD);
        add(matrix, "POST /api/bf4/subscriptions/configure",
                Protection.AUTHENTICATION,
                Protection.CSRF,
                Protection.USER_RATE_LIMIT,
                Protection.IP_RATE_LIMIT,
                Protection.PAYLOAD_LIMIT,
                Protection.OWNERSHIP,
                Protection.GENERIC_ERRORS);
        add(matrix, "GET /api/bf4/subscriptions",
                Protection.AUTHENTICATION,
                Protection.OWNERSHIP);
        add(matrix, "PATCH /api/bf4/subscriptions/{id}",
                Protection.AUTHENTICATION,
                Protection.CSRF,
                Protection.OWNERSHIP);
        add(matrix, "DELETE /api/bf4/subscriptions/{id}",
                Protection.AUTHENTICATION,
                Protection.CSRF,
                Protection.OWNERSHIP);
        add(matrix, "GET /api/bf4/notifications",
                Protection.AUTHENTICATION,
                Protection.OWNERSHIP,
                Protection.PAGINATION_LIMIT);
        add(matrix, "GET /api/bf4/subscriptions/{id}/notifications",
                Protection.AUTHENTICATION,
                Protection.OWNERSHIP,
                Protection.PAGINATION_LIMIT);
        add(matrix, "GET /actuator/health",
                Protection.SECURITY_HEADERS);
        add(matrix, "GET /actuator/info",
                Protection.AUTHENTICATION,
                Protection.AUTHORIZATION);
        add(matrix, "GET /actuator/metrics",
                Protection.AUTHENTICATION,
                Protection.AUTHORIZATION);

        return Collections.unmodifiableMap(matrix);
    }

    public static List<String> criticalSmokeChecks() {
        return List.of(
                "Unauthenticated protected request returns 401",
                "Cross-user resource returns 404",
                "Mutation without CSRF returns 403",
                "Unknown CORS origin is rejected",
                "Rate limit returns 429 with Retry-After",
                "Oversized page size returns 400",
                "Oversized JSON body returns 413",
                "Sensitive Actuator endpoint is unavailable",
                "Provider timeout is converted to a controlled failure",
                "Representative secrets are absent from sanitized logs"
        );
    }

    private static void add(
            Map<String, Set<Protection>> matrix,
            String endpoint,
            Protection... protections
    ) {
        LinkedHashSet<Protection> protectionSet = new LinkedHashSet<>();
        Collections.addAll(protectionSet, protections);
        matrix.put(endpoint, Collections.unmodifiableSet(protectionSet));
    }
}

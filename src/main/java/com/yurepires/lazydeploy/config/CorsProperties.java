package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

/** Configuração explícita das origens que podem consumir a API pelo navegador. */
@ConfigurationProperties(prefix = "lazydeploy.security.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        boolean allowCredentials,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        long maxAgeSeconds
) {

    public CorsProperties {
        allowedOrigins = normalizeOrigins(allowedOrigins);
        allowedMethods = normalizeValues(allowedMethods, "métodos CORS");
        allowedHeaders = normalizeValues(allowedHeaders, "headers CORS");
        exposedHeaders = normalizeValues(exposedHeaders, "headers CORS expostos");

        if (allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(
                    "É necessário configurar ao menos uma origem CORS permitida"
            );
        }
        if (allowCredentials && allowedOrigins.contains("*")) {
            throw new IllegalArgumentException(
                    "CORS com credenciais não pode utilizar origem curinga"
            );
        }
        if (maxAgeSeconds < 0) {
            throw new IllegalArgumentException(
                    "O cache do preflight CORS não pode ser negativo"
            );
        }
    }

    private static List<String> normalizeOrigins(List<String> origins) {
        if (origins == null) {
            return List.of();
        }

        return origins.stream()
                .map(CorsProperties::normalizeOrigin)
                .distinct()
                .toList();
    }

    private static String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException(
                    "A origem CORS não pode ser nula ou vazia"
            );
        }

        String normalizedOrigin = origin.trim();
        if ("*".equals(normalizedOrigin)) {
            return normalizedOrigin;
        }

        URI parsedOrigin;
        try {
            parsedOrigin = URI.create(normalizedOrigin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "A origem CORS deve ser uma URL válida com scheme e host",
                    exception
            );
        }

        if (!"http".equalsIgnoreCase(parsedOrigin.getScheme())
                && !"https".equalsIgnoreCase(parsedOrigin.getScheme())) {
            throw new IllegalArgumentException(
                    "A origem CORS deve utilizar http ou https"
            );
        }
        if (parsedOrigin.getHost() == null || parsedOrigin.getHost().isBlank()) {
            throw new IllegalArgumentException(
                    "A origem CORS deve conter um host"
            );
        }
        if (parsedOrigin.getUserInfo() != null
                || parsedOrigin.getQuery() != null
                || parsedOrigin.getFragment() != null
                || (parsedOrigin.getPath() != null && !parsedOrigin.getPath().isEmpty())) {
            throw new IllegalArgumentException(
                    "A origem CORS não pode conter caminho, credenciais, query ou fragmento"
            );
        }

        return normalizedOrigin;
    }

    private static List<String> normalizeValues(List<String> values, String description) {
        if (values == null) {
            return List.of();
        }

        List<String> normalizedValues = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "É necessário configurar ao menos um item para " + description
            );
        }
        return normalizedValues;
    }
}

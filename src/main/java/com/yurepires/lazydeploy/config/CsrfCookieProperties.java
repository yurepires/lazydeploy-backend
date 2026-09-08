package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração do cookie XSRF-TOKEN entregue à SPA. */
@ConfigurationProperties(prefix = "lazydeploy.security.csrf-cookie")
public record CsrfCookieProperties(
        boolean secure,
        boolean httpOnly,
        String sameSite,
        String path
) {

    public CsrfCookieProperties {
        sameSite = normalizeSameSite(sameSite, secure);
        path = normalizePath(path);

        if (httpOnly) {
            throw new IllegalArgumentException(
                    "O cookie XSRF-TOKEN precisa ser legível pelo Angular"
            );
        }
    }

    private static String normalizeSameSite(String value, boolean secure) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "SameSite do cookie CSRF deve ser configurado"
            );
        }

        String normalizedValue = value.trim();
        if (!normalizedValue.equalsIgnoreCase("lax")
                && !normalizedValue.equalsIgnoreCase("strict")
                && !normalizedValue.equalsIgnoreCase("none")) {
            throw new IllegalArgumentException(
                    "SameSite do cookie CSRF deve ser Lax, Strict ou None"
            );
        }
        if (normalizedValue.equalsIgnoreCase("none") && !secure) {
            throw new IllegalArgumentException(
                    "SameSite=None exige cookie CSRF Secure"
            );
        }
        if (normalizedValue.equalsIgnoreCase("none")) {
            return "None";
        }
        if (normalizedValue.equalsIgnoreCase("strict")) {
            return "Strict";
        }
        return "Lax";
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank() || !value.startsWith("/")) {
            throw new IllegalArgumentException(
                    "O path do cookie CSRF deve começar com '/'"
            );
        }
        return value.trim();
    }

}

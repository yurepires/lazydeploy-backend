package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração da integração HTTP com a API transacional do Mailjet. */
@ConfigurationProperties(prefix = "lazydeploy.mailjet")
public record MailjetProperties(
        String baseUrl,
        String apiKey,
        String apiSecret,
        String fromEmail,
        String fromName,
        int connectTimeoutMs,
        int responseTimeoutMs,
        long maxResponseBodyBytes
) {

    private static final String DEFAULT_BASE_URL = "https://api.mailjet.com";
    private static final String DEFAULT_FROM_NAME = "LazyDeploy";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3_000;
    private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 10_000;
    private static final long DEFAULT_MAX_RESPONSE_BODY_BYTES = 1_048_576;

    public MailjetProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = DEFAULT_FROM_NAME;
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        }
        if (responseTimeoutMs <= 0) {
            responseTimeoutMs = DEFAULT_RESPONSE_TIMEOUT_MS;
        }
        if (maxResponseBodyBytes <= 0) {
            maxResponseBodyBytes = DEFAULT_MAX_RESPONSE_BODY_BYTES;
        }
    }

    public boolean isConfigured() {
        return hasText(apiKey) && hasText(apiSecret) && hasText(fromEmail);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

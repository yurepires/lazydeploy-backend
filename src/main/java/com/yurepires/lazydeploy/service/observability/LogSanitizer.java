package com.yurepires.lazydeploy.service.observability;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Normaliza valores externos antes que sejam usados em logs.
 *
 * <p>Segredos não devem ser enviados a esta classe para serem "protegidos".
 * Ainda assim, a sanitização reduz o risco de log injection em valores que
 * precisam aparecer para diagnóstico, como nomes de operação e mensagens de
 * providers.</p>
 */
@Component
public class LogSanitizer {

    public static final int MAX_LOGGED_VALUE_LENGTH = 200;

    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n]+");
    private static final Pattern SENSITIVE_ASSIGNMENTS = Pattern.compile(
            "(?i)(password|passwd|secret|token|authorization|cookie|set-cookie)"
                    + "\\s*[:=]\\s*\\S+"
    );
    private static final Pattern UUID_SEGMENT = Pattern.compile(
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
    );
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("(?<=/)[0-9]+(?=/|$)");

    public String sanitize(String value) {
        return sanitize(value, MAX_LOGGED_VALUE_LENGTH);
    }

    public String sanitize(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        int safeMaximumLength = Math.max(1, maximumLength);
        String withoutLineBreaks = LINE_BREAKS.matcher(value).replaceAll(" ");
        String redacted = SENSITIVE_ASSIGNMENTS.matcher(withoutLineBreaks)
                .replaceAll("$1=[REDACTED]");
        if (redacted.length() <= safeMaximumLength) {
            return redacted;
        }
        return redacted.substring(0, safeMaximumLength);
    }

    public String normalizeEndpoint(String requestPath) {
        String sanitizedPath = sanitize(requestPath, MAX_LOGGED_VALUE_LENGTH);
        if (sanitizedPath.isBlank()) {
            return "/unknown";
        }

        int queryStart = sanitizedPath.indexOf('?');
        if (queryStart >= 0) {
            sanitizedPath = sanitizedPath.substring(0, queryStart);
        }
        int fragmentStart = sanitizedPath.indexOf('#');
        if (fragmentStart >= 0) {
            sanitizedPath = sanitizedPath.substring(0, fragmentStart);
        }

        String normalizedPath = UUID_SEGMENT.matcher(sanitizedPath).replaceAll("{id}");
        return NUMERIC_SEGMENT.matcher(normalizedPath).replaceAll("{id}");
    }
}

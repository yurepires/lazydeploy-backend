package com.yurepires.lazydeploy.service.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    private final LogSanitizer sanitizer = new LogSanitizer();

    @Test
    void shouldRemoveLineBreaksAndRedactSensitiveAssignments() {
        String sanitized = sanitizer.sanitize(
                "message=first\r\npassword=secret-value token=hidden"
        );

        assertThat(sanitized).doesNotContain("\r", "\n");
        assertThat(sanitized).contains("password=[REDACTED]");
        assertThat(sanitized).contains("token=[REDACTED]");
        assertThat(sanitized).doesNotContain("secret-value", "hidden");
    }

    @Test
    void shouldTruncateValuesAndNormalizeIdentifiersInEndpoints() {
        String oversized = "a".repeat(LogSanitizer.MAX_LOGGED_VALUE_LENGTH + 20);
        String endpoint = "/api/subscriptions/550e8400-e29b-41d4-a716-446655440000/42"
                + "?search=password=secret";

        assertThat(sanitizer.sanitize(oversized))
                .hasSize(LogSanitizer.MAX_LOGGED_VALUE_LENGTH);
        assertThat(sanitizer.normalizeEndpoint(endpoint))
                .isEqualTo("/api/subscriptions/{id}/{id}");
    }
}

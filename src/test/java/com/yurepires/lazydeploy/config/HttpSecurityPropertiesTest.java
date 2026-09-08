package com.yurepires.lazydeploy.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpSecurityPropertiesTest {

    @Test
    void shouldRejectWildcardCorsOriginWhenCredentialsAreEnabled() {
        assertThatThrownBy(() -> new CorsProperties(
                List.of("*"),
                true,
                List.of("GET"),
                List.of("Accept"),
                List.of("Retry-After"),
                3600
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origem curinga");
    }

    @Test
    void shouldRejectCorsOriginWithTrailingSlash() {
        assertThatThrownBy(() -> new CorsProperties(
                List.of("https://lazydeploy.pages.dev/"),
                true,
                List.of("GET"),
                List.of("Accept"),
                List.of("Retry-After"),
                3600
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("caminho");
    }

    @Test
    void shouldRequireSecureCookieForCrossSiteCsrf() {
        assertThatThrownBy(() -> new CsrfCookieProperties(
                false,
                false,
                "None",
                "/"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SameSite=None");
    }

    @Test
    void shouldRejectHttpOnlyCsrfCookie() {
        assertThatThrownBy(() -> new CsrfCookieProperties(
                true,
                true,
                "None",
                "/"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legível pelo Angular");
    }

    @Test
    void shouldRequirePositiveHstsDuration() {
        assertThatThrownBy(() -> new SecurityHeadersProperties(
                new SecurityHeadersProperties.HstsProperties(
                        true,
                        Duration.ZERO,
                        false,
                        false
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HSTS");
    }
}

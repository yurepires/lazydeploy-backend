package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuração dos headers HTTP adicionados pelo Spring Security. */
@ConfigurationProperties(prefix = "lazydeploy.security.headers")
public record SecurityHeadersProperties(HstsProperties hsts) {

    public SecurityHeadersProperties {
        if (hsts == null) {
            hsts = HstsProperties.defaults();
        }
    }

    public record HstsProperties(
            boolean enabled,
            Duration maxAge,
            boolean includeSubDomains,
            boolean preload
    ) {

        public HstsProperties {
            if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
                throw new IllegalArgumentException(
                        "A duração do HSTS deve ser positiva"
                );
            }
        }

        public static HstsProperties defaults() {
            return new HstsProperties(
                    false,
                    Duration.ofDays(365),
                    false,
                    false
            );
        }
    }
}

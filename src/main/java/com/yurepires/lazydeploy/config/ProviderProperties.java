package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Políticas técnicas para as integrações externas.
 *
 * <p>Cada provider possui limites próprios, pois o custo e a frequência das
 * chamadas de descoberta e monitoramento são diferentes.</p>
 */
@ConfigurationProperties(prefix = "lazydeploy.providers")
public record ProviderProperties(
        ProviderSettings gameTools,
        ProviderSettings keeper,
        ProviderSettings bflist
) {

    public ProviderProperties {
        gameTools = defaultWhenMissing(gameTools, ProviderSettings.gameToolsDefaults());
        keeper = defaultWhenMissing(keeper, ProviderSettings.keeperDefaults());
        bflist = defaultWhenMissing(bflist, ProviderSettings.bflistDefaults());
    }

    private static ProviderSettings defaultWhenMissing(
            ProviderSettings value,
            ProviderSettings defaultValue
    ) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public record ProviderSettings(
            @DefaultValue("2000") int connectTimeoutMs,
            @DefaultValue("5000") int responseTimeoutMs,
            @DefaultValue("10") int maxConcurrentRequests,
            @DefaultValue("2097152") long maxResponseBodyBytes,
            RetrySettings retry
    ) {

        public ProviderSettings(
                int connectTimeoutMs,
                int responseTimeoutMs,
                int maxConcurrentRequests,
                RetrySettings retry
        ) {
            this(
                    connectTimeoutMs,
                    responseTimeoutMs,
                    maxConcurrentRequests,
                    2_097_152,
                    retry
            );
        }

        public ProviderSettings {
            retry = defaultWhenMissing(retry, RetrySettings.disabled());
            validatePositive("connectTimeoutMs", connectTimeoutMs);
            validatePositive("responseTimeoutMs", responseTimeoutMs);
            validatePositive("maxConcurrentRequests", maxConcurrentRequests);
            validatePositive("maxResponseBodyBytes", maxResponseBodyBytes);
        }

        public static ProviderSettings gameToolsDefaults() {
            return new ProviderSettings(
                    2_000,
                    5_000,
                    10,
                    2_097_152,
                    RetrySettings.disabled()
            );
        }

        public static ProviderSettings keeperDefaults() {
            return new ProviderSettings(
                    2_000,
                    4_000,
                    20,
                    2_097_152,
                    RetrySettings.keeperDefaults()
            );
        }

        public static ProviderSettings bflistDefaults() {
            return new ProviderSettings(
                    2_000,
                    5_000,
                    5,
                    2_097_152,
                    RetrySettings.disabled()
            );
        }

        private static RetrySettings defaultWhenMissing(
                RetrySettings value,
                RetrySettings defaultValue
        ) {
            if (value == null) {
                return defaultValue;
            }
            return value;
        }

        private static void validatePositive(String fieldName, long value) {
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " deve ser positivo");
            }
        }
    }

    public record RetrySettings(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("1") int maxAttempts,
            @DefaultValue("250") long backoffMs
    ) {

        public RetrySettings(boolean enabled, int maxAttempts) {
            this(enabled, maxAttempts, defaultBackoff(enabled));
        }

        public RetrySettings {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts deve ser positivo");
            }
            if (backoffMs < 0) {
                throw new IllegalArgumentException("backoffMs não pode ser negativo");
            }
        }

        public static RetrySettings disabled() {
            return new RetrySettings(false, 1, 0);
        }

        public static RetrySettings keeperDefaults() {
            return new RetrySettings(true, 2, 250);
        }

        private static long defaultBackoff(boolean enabled) {
            if (enabled) {
                return 250;
            }
            return 0;
        }
    }
}

package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Limites internos usados para manter o consumo de recursos previsível.
 *
 * <p>Os valores ficam sob um prefixo próprio para que o dimensionamento possa
 * ser ajustado por ambiente sem alterar código.</p>
 */
@ConfigurationProperties(prefix = "lazydeploy.resources")
public record ResourceProperties(
        Database database,
        MonitoringExecutor monitoringExecutor,
        Http http,
        Shutdown shutdown
) {

    public ResourceProperties {
        database = valueOrDefault(database, Database.defaults());
        monitoringExecutor = valueOrDefault(
                monitoringExecutor,
                MonitoringExecutor.defaults()
        );
        http = valueOrDefault(http, Http.defaults());
        shutdown = valueOrDefault(shutdown, Shutdown.defaults());
    }

    public static ResourceProperties defaults() {
        return new ResourceProperties(
                Database.defaults(),
                MonitoringExecutor.defaults(),
                Http.defaults(),
                Shutdown.defaults()
        );
    }

    private static <T> T valueOrDefault(T value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public record Database(
            @DefaultValue("10") int maximumPoolSize,
            @DefaultValue("2") int minimumIdle,
            @DefaultValue("3000") long connectionTimeoutMs,
            @DefaultValue("600000") long idleTimeoutMs,
            @DefaultValue("1800000") long maxLifetimeMs
    ) {

        public Database {
            requirePositive("maximumPoolSize", maximumPoolSize);
            requireNonNegative("minimumIdle", minimumIdle);
            if (minimumIdle > maximumPoolSize) {
                throw new IllegalArgumentException(
                        "minimumIdle não pode ser maior que maximumPoolSize"
                );
            }
            requirePositive("connectionTimeoutMs", connectionTimeoutMs);
            requirePositive("idleTimeoutMs", idleTimeoutMs);
            requirePositive("maxLifetimeMs", maxLifetimeMs);
        }

        public static Database defaults() {
            return new Database(10, 2, 3_000, 600_000, 1_800_000);
        }
    }

    public record MonitoringExecutor(
            @DefaultValue("4") int corePoolSize,
            @DefaultValue("8") int maxPoolSize,
            @DefaultValue("100") int queueCapacity,
            @DefaultValue("60") int keepAliveSeconds
    ) {

        public MonitoringExecutor {
            requirePositive("corePoolSize", corePoolSize);
            requirePositive("maxPoolSize", maxPoolSize);
            requireNonNegative("queueCapacity", queueCapacity);
            requireNonNegative("keepAliveSeconds", keepAliveSeconds);
            if (corePoolSize > maxPoolSize) {
                throw new IllegalArgumentException(
                        "corePoolSize não pode ser maior que maxPoolSize"
                );
            }
        }

        public static MonitoringExecutor defaults() {
            return new MonitoringExecutor(4, 8, 100, 60);
        }
    }

    public record Http(
            @DefaultValue("50") int maxThreads,
            @DefaultValue("5") int minSpareThreads,
            @DefaultValue("200") int maxConnections,
            @DefaultValue("100") int acceptCount,
            @DefaultValue("5s") Duration connectionTimeout
    ) {

        public Http {
            requirePositive("maxThreads", maxThreads);
            requirePositive("minSpareThreads", minSpareThreads);
            requirePositive("maxConnections", maxConnections);
            requireNonNegative("acceptCount", acceptCount);
            if (connectionTimeout == null || connectionTimeout.isNegative()
                    || connectionTimeout.isZero()) {
                throw new IllegalArgumentException(
                        "connectionTimeout deve ser positivo"
                );
            }
        }

        public static Http defaults() {
            return new Http(50, 5, 200, 100, Duration.ofSeconds(5));
        }
    }

    public record Shutdown(
            @DefaultValue("20s") Duration timeout
    ) {

        public Shutdown {
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("timeout deve ser positivo");
            }
        }

        public static Shutdown defaults() {
            return new Shutdown(Duration.ofSeconds(20));
        }
    }

    private static void requirePositive(String fieldName, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " deve ser positivo");
        }
    }

    private static void requireNonNegative(String fieldName, long value) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " não pode ser negativo");
        }
    }
}

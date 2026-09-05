package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limites de quantidade e tamanho aplicados aos recursos da API.
 *
 * <p>Esses valores fazem parte do contrato de negócio e, por isso, ficam
 * centralizados e configuráveis em vez de espalhados pelos controllers.</p>
 */
@ConfigurationProperties(prefix = "lazydeploy.limits")
public record BusinessLimitProperties(
        @DefaultValue("20") int subscriptionsPerUser,
        @DefaultValue("10") int rulesPerSubscription,
        @DefaultValue("5") int channelsPerSubscription,
        @DefaultValue("20") int mapsPerMapInRule,
        @DefaultValue("2") int serverSearchQueryMinLength,
        @DefaultValue("100") int serverSearchQueryMaxLength,
        @DefaultValue("20") int defaultPageSize,
        @DefaultValue("100") int maxPageSize,
        @DefaultValue("10000") int maxPageNumber,
        @DefaultValue("1048576") long maxRequestBodyBytes
) {

    public BusinessLimitProperties {
        requirePositive("subscriptionsPerUser", subscriptionsPerUser);
        requirePositive("rulesPerSubscription", rulesPerSubscription);
        requirePositive("channelsPerSubscription", channelsPerSubscription);
        requirePositive("mapsPerMapInRule", mapsPerMapInRule);
        requirePositive("serverSearchQueryMinLength", serverSearchQueryMinLength);
        requirePositive("serverSearchQueryMaxLength", serverSearchQueryMaxLength);
        requirePositive("defaultPageSize", defaultPageSize);
        requirePositive("maxPageSize", maxPageSize);
        requirePositive("maxRequestBodyBytes", maxRequestBodyBytes);

        if (serverSearchQueryMinLength > serverSearchQueryMaxLength) {
            throw new IllegalArgumentException(
                    "serverSearchQueryMinLength não pode ser maior que serverSearchQueryMaxLength"
            );
        }

        if (defaultPageSize > maxPageSize) {
            throw new IllegalArgumentException(
                    "defaultPageSize não pode ser maior que maxPageSize"
            );
        }

        if (maxPageNumber < 0) {
            throw new IllegalArgumentException("maxPageNumber não pode ser negativo");
        }
    }

    private static void requirePositive(String fieldName, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " deve ser positivo");
        }
    }
}

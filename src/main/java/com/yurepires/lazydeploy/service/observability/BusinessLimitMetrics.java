package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/** Métricas agregadas das rejeições por limites de negócio. */
@Component
public class BusinessLimitMetrics {

    public static final String REJECTIONS =
            "lazydeploy.security.business_limit.rejections";

    private static final Set<String> ALLOWED_LIMIT_TYPES = Set.of(
            "subscriptions",
            "rules",
            "channels",
            "maps",
            "search_query",
            "page_size",
            "request_body"
    );

    private final MeterRegistry meterRegistry;

    public BusinessLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRejection(String limitType) {
        if (meterRegistry == null) {
            return;
        }

        String normalizedType = normalizeLimitType(limitType);
        try {
            Counter.builder(REJECTIONS)
                    .tag("limit_type", normalizedType)
                    .description("Requisições rejeitadas por limites de negócio")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Métricas não devem interromper o processamento da requisição.
        }
    }

    private String normalizeLimitType(String limitType) {
        if (limitType == null) {
            return "request_body";
        }

        String normalizedType = limitType.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_LIMIT_TYPES.contains(normalizedType)) {
            return normalizedType;
        }
        return "request_body";
    }
}

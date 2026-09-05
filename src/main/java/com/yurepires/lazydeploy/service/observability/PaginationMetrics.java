package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/** Métricas de paginação rejeitada, sem dados de identificação da requisição. */
@Component
public class PaginationMetrics {

    public static final String REJECTIONS = "lazydeploy.http.pagination.rejections";

    private static final Set<String> ALLOWED_REASONS = Set.of(
            "invalid_page",
            "invalid_page_size",
            "page_size_limit_exceeded"
    );

    private final MeterRegistry meterRegistry;

    public PaginationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRejection(String reason) {
        if (meterRegistry == null) {
            return;
        }

        String normalizedReason = normalizeReason(reason);
        try {
            Counter.builder(REJECTIONS)
                    .tag("reason", normalizedReason)
                    .description("Requisições rejeitadas por paginação inválida")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Métricas não devem interromper o processamento da requisição.
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return "invalid_page";
        }

        String normalizedReason = reason.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_REASONS.contains(normalizedReason)) {
            return normalizedReason;
        }
        return "invalid_page";
    }
}

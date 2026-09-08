package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/** Métricas de saturação dos recursos internos da aplicação. */
@Component
public class ResourceMetrics {

    public static final String EXECUTOR_REJECTIONS =
            "lazydeploy.executor.rejections";
    public static final String SATURATION = "lazydeploy.resources.saturation";
    public static final String EXECUTOR_ACTIVE = "lazydeploy.executor.active";
    public static final String EXECUTOR_POOL_SIZE = "lazydeploy.executor.pool.size";
    public static final String EXECUTOR_QUEUE_SIZE = "lazydeploy.executor.queue.size";
    public static final String EXECUTOR_COMPLETED_TASKS =
            "lazydeploy.executor.completed.tasks";

    private final MeterRegistry meterRegistry;

    public ResourceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static ResourceMetrics noop() {
        return new ResourceMetrics(null);
    }

    public void recordExecutorRejection(String executorName) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(EXECUTOR_REJECTIONS)
                    .tag("executor", normalizeExecutorName(executorName))
                    .description("Tarefas rejeitadas por executors saturados")
                    .register(meterRegistry)
                    .increment();
            Counter.builder(SATURATION)
                    .tag("resource", "monitoring_executor")
                    .description("Eventos de saturação de recursos internos")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ignored) {
            // Observabilidade não pode interromper a política de rejeição.
        }
    }

    public void bindExecutor(String executorName, ThreadPoolExecutor executor) {
        if (meterRegistry == null || executor == null) {
            return;
        }

        String normalizedName = normalizeExecutorName(executorName);
        try {
            registerGauge(EXECUTOR_ACTIVE, normalizedName, executor,
                    ThreadPoolExecutor::getActiveCount);
            registerGauge(EXECUTOR_POOL_SIZE, normalizedName, executor,
                    ThreadPoolExecutor::getPoolSize);
            registerGauge(EXECUTOR_QUEUE_SIZE, normalizedName, executor,
                    value -> value.getQueue().size());
            registerGauge(EXECUTOR_COMPLETED_TASKS, normalizedName, executor,
                    value -> value.getCompletedTaskCount());
        } catch (RuntimeException ignored) {
            // Métricas não podem impedir a inicialização do executor.
        }
    }

    private void registerGauge(
            String metricName,
            String executorName,
            ThreadPoolExecutor executor,
            java.util.function.ToDoubleFunction<ThreadPoolExecutor> valueFunction
    ) {
        Gauge.builder(metricName, executor, valueFunction)
                .tag("executor", executorName)
                .register(meterRegistry);
    }

    private String normalizeExecutorName(String executorName) {
        if (executorName == null || executorName.isBlank()) {
            return "unknown";
        }
        return switch (executorName.trim()) {
            case "monitoring" -> "monitoring";
            default -> "unknown";
        };
    }
}

package com.yurepires.lazydeploy.service.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMetricsTest {

    @Test
    void shouldExposeExecutorSaturationMetricsWithoutDynamicTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ResourceMetrics metrics = new ResourceMetrics(registry);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2)
        );

        try {
            metrics.bindExecutor("monitoring", executor);
            metrics.recordExecutorRejection("unexpected-executor");

            assertThat(registry.get(ResourceMetrics.EXECUTOR_REJECTIONS)
                    .tag("executor", "unknown")
                    .counter()
                    .count()).isEqualTo(1);
            assertThat(registry.get(ResourceMetrics.EXECUTOR_QUEUE_SIZE)
                    .tag("executor", "monitoring")
                    .gauge()
                    .value()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }
}

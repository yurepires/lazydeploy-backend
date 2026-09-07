package com.yurepires.lazydeploy.config;

import com.yurepires.lazydeploy.service.observability.ResourceMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceLimitConfigurationTest {

    @Test
    void shouldRejectWorkWhenMonitoringExecutorPoolAndQueueAreFull() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ResourceMetrics metrics = new ResourceMetrics(registry);
        ResourceLimitConfiguration configuration = new ResourceLimitConfiguration();
        ResourceProperties properties = new ResourceProperties(
                ResourceProperties.Database.defaults(),
                new ResourceProperties.MonitoringExecutor(1, 1, 1, 0),
                ResourceProperties.Http.defaults(),
                new ResourceProperties.Shutdown(Duration.ofSeconds(1))
        );
        ThreadPoolTaskExecutor executor = configuration.monitoringTaskExecutor(
                properties,
                metrics
        );
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);

        try {
            executor.execute(() -> {
                taskStarted.countDown();
                await(releaseTask);
            });
            assertThat(taskStarted.await(1, java.util.concurrent.TimeUnit.SECONDS))
                    .isTrue();
            executor.execute(() -> {
                // Ocupa a única posição da fila.
            });

            assertThatThrownBy(() -> executor.execute(() -> {
                // Deve ser rejeitada enquanto pool e fila estiverem ocupados.
            }))
                    .isInstanceOf(RejectedExecutionException.class);
            assertThat(registry.get(ResourceMetrics.EXECUTOR_REJECTIONS)
                    .tag("executor", "monitoring")
                    .counter()
                    .count()).isEqualTo(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("O teste foi interrompido", exception);
        } finally {
            releaseTask.countDown();
            executor.shutdown();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}

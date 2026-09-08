package com.yurepires.lazydeploy.config;

import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import com.yurepires.lazydeploy.service.observability.ResourceMetrics;
import com.yurepires.lazydeploy.service.observability.SecurityEventLogger;
import com.yurepires.lazydeploy.service.observability.SecurityEventOutcome;
import com.yurepires.lazydeploy.service.observability.SecurityEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

import java.util.concurrent.RejectedExecutionException;

/** Configura pools finitos para scheduler e tarefas de monitoramento. */
@Configuration
@ConditionalOnClass({
        ThreadPoolTaskExecutor.class,
        TomcatServletWebServerFactory.class
})
public class ResourceLimitConfiguration {

    private static final Logger log = LoggerFactory.getLogger(
            ResourceLimitConfiguration.class
    );

    @Bean(name = "monitoringTaskExecutor")
    public ThreadPoolTaskExecutor monitoringTaskExecutor(
            ResourceProperties properties,
            ResourceMetrics resourceMetrics,
            SecurityEventLogger securityEventLogger
    ) {
        return createMonitoringTaskExecutor(
                properties,
                resourceMetrics,
                securityEventLogger
        );
    }

    /** Mantém uma construção simples para testes unitários sem o contexto Spring. */
    public ThreadPoolTaskExecutor monitoringTaskExecutor(
            ResourceProperties properties,
            ResourceMetrics resourceMetrics
    ) {
        return createMonitoringTaskExecutor(
                properties,
                resourceMetrics,
                new SecurityEventLogger(new LogSanitizer())
        );
    }

    private ThreadPoolTaskExecutor createMonitoringTaskExecutor(
            ResourceProperties properties,
            ResourceMetrics resourceMetrics,
            SecurityEventLogger securityEventLogger
    ) {
        ResourceProperties.MonitoringExecutor settings =
                properties.monitoringExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(settings.corePoolSize());
        executor.setMaxPoolSize(settings.maxPoolSize());
        executor.setQueueCapacity(settings.queueCapacity());
        executor.setKeepAliveSeconds(settings.keepAliveSeconds());
        executor.setThreadNamePrefix("lazydeploy-monitoring-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownSeconds(properties));
        executor.setRejectedExecutionHandler((task, threadPoolExecutor) -> {
            resourceMetrics.recordExecutorRejection("monitoring");
            securityEventLogger.log(
                    SecurityEventType.RESOURCE_SATURATION,
                    SecurityEventOutcome.REJECTED,
                    "MONITORING_EXECUTOR",
                    "/internal/monitoring/executor"
            );
            throw new RejectedExecutionException(
                    "Executor de monitoramento está saturado"
            );
        });
        executor.initialize();

        resourceMetrics.bindExecutor(
                "monitoring",
                executor.getThreadPoolExecutor()
        );
        return executor;
    }

    /**
     * O monitor possui um único trigger agendado. Um scheduler de uma thread
     * evita que o próprio mecanismo de agendamento crie concorrência implícita.
     */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler(ResourceProperties properties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("lazydeploy-scheduler-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(shutdownSeconds(properties));
        scheduler.setErrorHandler(schedulerErrorHandler());
        return scheduler;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory>
    tomcatResourceLimits(ResourceProperties properties) {
        ResourceProperties.Http settings = properties.http();
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setProperty("maxThreads", String.valueOf(settings.maxThreads()));
            connector.setProperty(
                    "minSpareThreads",
                    String.valueOf(settings.minSpareThreads())
            );
            connector.setProperty(
                    "maxConnections",
                    String.valueOf(settings.maxConnections())
            );
            connector.setProperty("acceptCount", String.valueOf(settings.acceptCount()));
            connector.setProperty(
                    "connectionTimeout",
                    String.valueOf(settings.connectionTimeout().toMillis())
            );
        });
    }

    private ErrorHandler schedulerErrorHandler() {
        return throwable -> log.error(
                "Erro não tratado no scheduler de monitoramento | exceptionType={}",
                throwable.getClass().getSimpleName(),
                throwable
        );
    }

    private int shutdownSeconds(ResourceProperties properties) {
        long seconds = properties.shutdown().timeout().toSeconds();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, seconds));
    }
}

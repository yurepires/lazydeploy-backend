package com.yurepires.lazydeploy.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Aplica limites explícitos ao pool Hikari usado pelo JPA e pelo Flyway. */
@Configuration
@ConditionalOnClass(HikariDataSource.class)
public class DataSourceResourceConfiguration {

    @Bean
    public HikariDataSource dataSource(
            DataSourceProperties dataSourceProperties,
            ResourceProperties resourceProperties
    ) {
        ResourceProperties.Database settings = resourceProperties.database();
        HikariDataSource dataSource = dataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        dataSource.setPoolName("lazydeploy-database-pool");
        dataSource.setMaximumPoolSize(settings.maximumPoolSize());
        dataSource.setMinimumIdle(settings.minimumIdle());
        dataSource.setConnectionTimeout(settings.connectionTimeoutMs());
        dataSource.setIdleTimeout(settings.idleTimeoutMs());
        dataSource.setMaxLifetime(settings.maxLifetimeMs());
        return dataSource;
    }
}

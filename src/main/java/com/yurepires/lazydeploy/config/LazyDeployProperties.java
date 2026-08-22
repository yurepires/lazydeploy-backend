package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lazydeploy")
public record LazyDeployProperties(
        MonitoringProperties monitoring,
        String gameToolsBaseUrl,
        String battlelogKeeperBaseUrl,
        String emailFrom
) {}

package com.yurepires.lazydeploy.config;

import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "lazydeploy")
public record LazyDeployProperties(
        String baseUrl,
        Api api,
        Monitoring monitoring,
        List<MonitoredServer> servers,
        Channels channels
) {
    public LazyDeployProperties {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    public record Api(int pageSize) {}

    public record Monitoring(Duration interval) {}

    public record Channels(Email email) {}

    public record Email(String from) {}
}

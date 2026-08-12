package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "bf4")
public record Bf4Properties (
        String baseUrl,
        Monitoring monitoring,
        Api api,
        List<MonitoredServer> monitoredServers
) {

    public Bf4Properties {
        monitoredServers = monitoredServers == null ? List.of() : List.copyOf(monitoredServers);
    }

    public record Monitoring(
            Duration interval
    ){}

    public record Api(
            int pageSize
    ){}

    public record MonitoredServer(
            String guid,
            String ip,
            int port,
            String name,
            boolean enabled,
            int minPlayers,
            Set<String> favoriteMaps
    ) {
        public MonitoredServer {
            if (minPlayers < 0) {
                throw new IllegalArgumentException("minPlayers não pode ser negativo");
            }
            favoriteMaps = favoriteMaps == null ? Set.of() : Set.copyOf(favoriteMaps);
        }
    }

}

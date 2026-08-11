package com.yurepires.lazydeploy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "bf4")
public record Bf4Properties (
        String baseUrl,
        Server server,
        Monitoring monitoring,
        Api api
) {

    public record Server(
            String guid
    ){}

    public record Monitoring(
            Duration interval
    ){}

    public record Api(
            int pageSize
    ){}

}

package com.yurepires.lazydeploy.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;

@Configuration
public class HttpClientConfig {

    @Bean
    @Qualifier("bfListWebClient")
    public WebClient bfListWebClient(LazyDeployProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    @Qualifier("gameToolsWebClient")
    public WebClient gameToolsWebClient(LazyDeployProperties properties) {
        return WebClient.builder().baseUrl(properties.providers().gameTools().baseUrl()).build();
    }

    @Bean
    @Qualifier("keeperWebClient")
    public WebClient keeperWebClient(LazyDeployProperties properties) {
        return WebClient.builder().baseUrl(properties.providers().battlelogKeeper().baseUrl()).build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}

package com.yurepires.lazydeploy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public WebClient bfListWebClient(Bf4Properties properties) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

}

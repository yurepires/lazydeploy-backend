package com.yurepires.lazydeploy.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    @Qualifier("gameToolsWebClient")
    public WebClient gameToolsWebClient(
            LazyDeployProperties properties,
            ProviderProperties providerProperties
    ) {
        return createClient(
                properties.gameToolsBaseUrl(),
                providerProperties.gameTools()
        );
    }

    public WebClient gameToolsWebClient(LazyDeployProperties properties) {
        return createClient(
                properties.gameToolsBaseUrl(),
                ProviderProperties.ProviderSettings.gameToolsDefaults()
        );
    }

    @Bean
    @Qualifier("keeperWebClient")
    public WebClient keeperWebClient(
            LazyDeployProperties properties,
            ProviderProperties providerProperties
    ) {
        return createClient(
                properties.battlelogKeeperBaseUrl(),
                providerProperties.keeper()
        );
    }

    public WebClient keeperWebClient(LazyDeployProperties properties) {
        return createClient(
                properties.battlelogKeeperBaseUrl(),
                ProviderProperties.ProviderSettings.keeperDefaults()
        );
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    private WebClient createClient(
            String baseUrl,
            ProviderProperties.ProviderSettings settings
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        settings.connectTimeoutMs()
                )
                .responseTimeout(Duration.ofMillis(settings.responseTimeoutMs()));

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(
                        toInteger(settings.maxResponseBodyBytes())
                ))
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .build();
    }

    private int toInteger(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

}

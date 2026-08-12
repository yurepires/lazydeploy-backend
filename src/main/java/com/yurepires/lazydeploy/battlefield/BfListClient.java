package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerPageResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class BfListClient {

    private final WebClient webClient;
    private final Bf4Properties properties;

    public BfListClient(WebClient webClient, Bf4Properties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public Bf4ServerPageResponse getFirstServerPage() {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/servers")
                        .queryParam("perPage", properties.api().pageSize())
                        .build())
                .retrieve()
                .bodyToMono(Bf4ServerPageResponse.class)
                .block();
    }

    public Bf4ServerPageResponse getNextServerPage(String cursor, String after) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/servers")
                        .queryParam("perPage", properties.api().pageSize())
                        .queryParam("cursor", cursor)
                        .queryParam("after", after)
                        .build())
                .retrieve()
                .bodyToMono(Bf4ServerPageResponse.class)
                .block();
    }

}

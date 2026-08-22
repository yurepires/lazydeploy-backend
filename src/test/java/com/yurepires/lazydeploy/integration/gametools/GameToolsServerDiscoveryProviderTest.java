package com.yurepires.lazydeploy.integration.gametools;


import com.yurepires.lazydeploy.model.server.ServerSearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class GameToolsServerDiscoveryProviderTest {
    @Test
    void shouldReturnAllResultsAndMapBattlelogIdAsExternalGuid() {
        String response = """
                {"servers":[
                  {
                    "prefix":"First",
                    "battlelogId":"guid-1",
                    "serverId":"guid-1",
                    "currentMap":"Map one",
                    "playerAmount":10,
                    "maxPlayers":64,
                    "inQue":1
                  },
                  {
                    "prefix":"Second",
                    "battlelogId":"guid-2",
                    "serverId":"guid-2",
                    "currentMap":"Map two",
                    "playerAmount":20,
                    "maxPlayers":64,
                    "inQue":2
                  }
                ]}
                """;

        var results = provider(response).search(new ServerSearchQuery("server", 10));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(server -> server.externalGuid()).containsExactly("guid-1", "guid-2");
        assertThat(results).extracting(server -> server.displayName()).containsExactly("First", "Second");
    }

    @Test
    void shouldReturnEmptyWhenNoServerMatches() {
        assertThat(provider("{\"servers\":[]}").search(new ServerSearchQuery("missing", 10))).isEmpty();
    }

    private GameToolsServerDiscoveryProvider provider(String json) {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build()))
                .build();
        return new GameToolsServerDiscoveryProvider(client);
    }
}

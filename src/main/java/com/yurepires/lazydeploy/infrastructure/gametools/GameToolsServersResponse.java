package com.yurepires.lazydeploy.infrastructure.gametools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameToolsServersResponse(List<GameToolsServerResponse> servers) {
    public GameToolsServersResponse {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }
}

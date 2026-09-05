package com.yurepires.lazydeploy.integration.gametools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameToolsServersResponse(List<GameToolsServerResponse> servers) {
}

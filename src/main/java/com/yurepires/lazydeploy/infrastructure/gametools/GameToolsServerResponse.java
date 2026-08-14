package com.yurepires.lazydeploy.infrastructure.gametools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameToolsServerResponse(
        String prefix,
        String battlelogId,
        String serverId,
        String currentMap,
        Integer playerAmount,
        Integer maxPlayers,
        Integer inQue,
        String mode,
        String country,
        String region,
        String platform,
        String serverLink
) {}

package com.yurepires.lazydeploy.dto;

import java.time.Instant;

public record MapChangedEvent(
        String serverGuid,
        String serverName,
        String previousMap,
        String currentMap,
        String currentMapLabel,
        int players,
        int maxPlayers,
        int roundTime,
        Instant detectedAt
) {}
package com.yurepires.lazydeploy.monitoring.event;

import java.time.Instant;

public record MapChangedEvent(
        String serverGuid,
        String serverName,
        String serverIp,
        int serverPort,
        String previousMap,
        String previousMapLabel,
        String currentMap,
        String currentMapLabel,
        int numPlayers,
        int maxPlayers,
        String gameType,
        int roundTime,
        Instant detectedAt
) {}

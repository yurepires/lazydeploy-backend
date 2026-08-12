package com.yurepires.lazydeploy.notification;

import java.time.Instant;

public record NotificationCandidate(
        String serverGuid,
        String serverName,
        String map,
        String mapLabel,
        int players,
        int maxPlayers,
        String gameType,
        Instant detectedAt
) {}

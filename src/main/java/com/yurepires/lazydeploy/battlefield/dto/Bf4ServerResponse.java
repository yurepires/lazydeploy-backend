package com.yurepires.lazydeploy.battlefield.dto;

public record Bf4ServerResponse(
        String guid,
        String ip,
        int port,
        String name,
        int numPlayers,
        int maxPlayers,
        String map,
        String mapLabel,
        String gameType,
        int roundsPlayed,
        int roundsTotal,
        int roundTime
) {}

package com.yurepires.lazydeploy.infrastructure.battlelogkeeper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeeperSnapshot(
        String status,
        Long gameId,
        String gameMode,
        Integer mapVariant,
        String currentMap,
        int maxPlayers,
        int waitingPlayers,
        long roundTime,
        Integer defaultRoundTimeMultiplier,
        Map<String, KeeperTeamInfo> teamInfo,
        Map<String, KeeperTicketInfo> conquest
) {}

package com.yurepires.lazydeploy.integration.battlelogkeeper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeeperSnapshot(
        String status,
        Long gameId,
        String gameMode,
        Integer mapVariant,
        String currentMap,
        Integer maxPlayers,
        Integer waitingPlayers,
        Long roundTime,
        Integer defaultRoundTimeMultiplier,
        Map<String, KeeperTeamInfo> teamInfo,
        Map<String, KeeperTicketInfo> conquest
) {}

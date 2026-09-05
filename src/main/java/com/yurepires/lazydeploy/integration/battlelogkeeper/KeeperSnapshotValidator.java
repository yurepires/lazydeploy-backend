package com.yurepires.lazydeploy.integration.battlelogkeeper;

import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshot;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshotResponse;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperTeamInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Valida a estrutura mínima antes de transformar uma resposta em snapshot. */
@Component
public class KeeperSnapshotValidator {

    private final MapIdentifierNormalizer mapIdentifierNormalizer;

    public KeeperSnapshotValidator() {
        this(new MapIdentifierNormalizer());
    }

    @Autowired
    public KeeperSnapshotValidator(MapIdentifierNormalizer mapIdentifierNormalizer) {
        this.mapIdentifierNormalizer = mapIdentifierNormalizer;
    }

    public boolean isValid(KeeperSnapshotResponse response) {
        if (response == null || response.snapshot() == null) {
            return false;
        }

        KeeperSnapshot snapshot = response.snapshot();
        if (!"SUCCESS".equalsIgnoreCase(snapshot.status())) {
            return false;
        }
        if (snapshot.currentMap() == null || snapshot.currentMap().isBlank()) {
            return false;
        }
        if (mapIdentifierNormalizer.normalize(snapshot.currentMap()) == null) {
            return false;
        }
        if (snapshot.maxPlayers() == null || snapshot.maxPlayers() < 0) {
            return false;
        }
        if (snapshot.waitingPlayers() == null || snapshot.waitingPlayers() < 0) {
            return false;
        }
        if (snapshot.roundTime() == null || snapshot.roundTime() < 0) {
            return false;
        }

        return hasValidTeamInfo(snapshot.teamInfo());
    }

    private boolean hasValidTeamInfo(Map<String, KeeperTeamInfo> teamInfo) {
        if (teamInfo == null) {
            return false;
        }

        for (Map.Entry<String, KeeperTeamInfo> entry : teamInfo.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                return false;
            }
            if (entry.getValue().players() == null) {
                return false;
            }
        }

        return true;
    }
}

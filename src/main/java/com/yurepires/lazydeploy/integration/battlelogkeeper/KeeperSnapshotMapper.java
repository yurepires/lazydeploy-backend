package com.yurepires.lazydeploy.integration.battlelogkeeper;

import com.yurepires.lazydeploy.model.server.MapSnapshot;
import com.yurepires.lazydeploy.model.server.PlayerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshot;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshotResponse;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperTeamInfo;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class KeeperSnapshotMapper {

    private final MapIdentifierNormalizer normalizer;
    private final Clock clock;

    public KeeperSnapshotMapper(MapIdentifierNormalizer normalizer, Clock clock) {
        this.normalizer = normalizer;
        this.clock = clock;
    }

    public ServerSnapshot map(ServerReference server, KeeperSnapshotResponse response) {
        KeeperSnapshot snapshot = response.snapshot();
        String normalizedMap = normalizer.normalize(snapshot.currentMap());
        Map<String, Object> metadata = new LinkedHashMap<>();
        put(metadata, "gameId", snapshot.gameId());
        put(metadata, "mapVariant", snapshot.mapVariant());
        put(metadata, "defaultRoundTimeMultiplier", snapshot.defaultRoundTimeMultiplier());
        put(metadata, "lastUpdated", response.lastUpdated());
        put(metadata, "conquest", snapshot.conquest());

        return new ServerSnapshot(
                server.externalGuid(),
                new MapSnapshot(snapshot.currentMap(), normalizedMap, null),
                new PlayerSnapshot(countPlayers(snapshot.teamInfo()), snapshot.maxPlayers(), snapshot.waitingPlayers()),
                snapshot.gameMode(),
                snapshot.roundTime(),
                Instant.now(clock),
                metadata
        );
    }

    private int countPlayers(Map<String, KeeperTeamInfo> teams) {
        if (teams == null) {
            return 0;
        }
        return teams.values().stream()
                .filter(team -> team != null && team.players() != null)
                .mapToInt(team -> team.players().size())
                .sum();
    }

    private void put(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}

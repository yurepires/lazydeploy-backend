package com.yurepires.lazydeploy.integration.battlelogkeeper;


import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshot;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperSnapshotResponse;
import com.yurepires.lazydeploy.integration.battlelogkeeper.dto.KeeperTeamInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeeperSnapshotMapperTest {
    @Test
    void shouldNormalizeMapCountEveryTeamAndKeepGameIdOnlyAsMetadata() {
        Instant capturedAt = Instant.parse("2026-08-13T12:00:00Z");
        KeeperSnapshotMapper mapper = new KeeperSnapshotMapper(
                new MapIdentifierNormalizer(), Clock.fixed(capturedAt, ZoneOffset.UTC)
        );
        KeeperSnapshot keeper = new KeeperSnapshot(
                "SUCCESS", 123L, "ConquestLarge", 0,
                "XP0/Levels/MP_Subway/XP0_Metro/", 64, 3, 987L, 100,
                Map.of(
                        "0", new KeeperTeamInfo(0, Map.of("spectator", new Object())),
                        "1", new KeeperTeamInfo(1, Map.of("one", new Object(), "two", new Object())),
                        "2", new KeeperTeamInfo(2, Map.of("three", new Object()))
                ),
                Map.of()
        );

        var result = mapper.map(
                new ServerReference(null, "guid", "Server", "TEST", Map.of()),
                new KeeperSnapshotResponse(99L, keeper)
        );

        assertThat(result.serverGuid()).isEqualTo("guid");
        assertThat(result.map().externalId()).isEqualTo("XP0/Levels/MP_Subway/XP0_Metro/");
        assertThat(result.map().normalizedId()).isEqualTo("XP0_Metro");
        assertThat(result.players().current()).isEqualTo(4);
        assertThat(result.players().waiting()).isEqualTo(3);
        assertThat(result.roundTimeSeconds()).isEqualTo(987);
        assertThat(result.capturedAt()).isEqualTo(capturedAt);
        assertThat(result.externalMetadata()).containsEntry("gameId", 123L);
    }
}

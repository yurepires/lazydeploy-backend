package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.dto.response.CurrentServerStatusResponse;
import com.yurepires.lazydeploy.model.map.BattlefieldMap;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CurrentServerStatusMapperTest {

    private final CurrentServerStatusMapper mapper = new CurrentServerStatusMapper(
            mock(MapCatalogService.class)
    );

    @Test
    void shouldReturnUnavailableWhenServerStateIsMissing() {
        CurrentServerStatusResponse response = mapper.toResponse(null, Map.of());

        assertThat(response.available()).isFalse();
        assertThat(response.availabilityReason()).isEqualTo("NOT_OBSERVED_YET");
        assertThat(response.map()).isNull();
        assertThat(response.players()).isNull();
        assertThat(response.gameMode()).isNull();
    }

    @Test
    void shouldMapCurrentServerState() {
        Instant observedAt = Instant.parse("2026-09-02T23:45:12Z");
        ServerState state = state("MP_Prison", observedAt, 54, 64, "ConquestLarge0");

        CurrentServerStatusResponse response = mapper.toResponse(
                state,
                mapper.createMapDisplayNameIndex(List.of(
                        new BattlefieldMap("MP_Prison", "Operation Locker", true, null, Map.of())
                ))
        );

        assertThat(response.available()).isTrue();
        assertThat(response.availabilityReason()).isNull();
        assertThat(response.map().id()).isEqualTo("MP_Prison");
        assertThat(response.map().displayName()).isEqualTo("Operation Locker");
        assertThat(response.players().current()).isEqualTo(54);
        assertThat(response.players().max()).isEqualTo(64);
        assertThat(response.gameMode()).isEqualTo("ConquestLarge0");
        assertThat(response.lastObservedAt()).isEqualTo(observedAt);
        assertThat(response.capturedAt()).isEqualTo(observedAt);
    }

    @Test
    void shouldFallbackToMapIdWhenCatalogDoesNotKnowMap() {
        CurrentServerStatusResponse response = mapper.toResponse(
                state("XP9_NewMap", Instant.now(), 1, 64, null),
                Map.of()
        );

        assertThat(response.available()).isTrue();
        assertThat(response.map().id()).isEqualTo("XP9_NewMap");
        assertThat(response.map().displayName()).isEqualTo("XP9_NewMap");
    }

    private ServerState state(
            String mapId,
            Instant observedAt,
            Integer playerCount,
            Integer maxPlayers,
            String gameMode
    ) {
        return new ServerState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                mapId,
                120,
                observedAt,
                observedAt,
                playerCount,
                maxPlayers,
                gameMode
        );
    }
}

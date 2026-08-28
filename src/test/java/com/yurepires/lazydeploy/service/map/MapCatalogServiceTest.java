package com.yurepires.lazydeploy.service.map;

import com.yurepires.lazydeploy.entity.BattlefieldMapEntity;
import com.yurepires.lazydeploy.mapper.BattlefieldMapMapper;
import com.yurepires.lazydeploy.model.map.BattlefieldMap;
import com.yurepires.lazydeploy.repository.BattlefieldMapRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapCatalogServiceTest {

    private final BattlefieldMapRepository repository = mock(BattlefieldMapRepository.class);
    private final BattlefieldMapMapper mapper = mock(BattlefieldMapMapper.class);
    private final MapCatalogService service = new MapCatalogService(repository, mapper);

    @Test
    void shouldResolveDisplayNameFromTechnicalId() {
        BattlefieldMapEntity entity = entity("MP_Prison", "Operation Locker", true);
        BattlefieldMap map = map("MP_Prison", "Operation Locker", true);

        when(repository.findById("MP_Prison")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(map);

        assertThat(service.getDisplayName("MP_Prison")).isEqualTo("Operation Locker");
    }

    @Test
    void shouldUseTechnicalIdWhenMapIsUnknown() {
        when(repository.findById("MAP_UNKNOWN")).thenReturn(Optional.empty());

        assertThat(service.getDisplayName("MAP_UNKNOWN")).isEqualTo("MAP_UNKNOWN");
    }

    @Test
    void shouldListEnabledMapsOrderedByDisplayName() {
        BattlefieldMapEntity locker = entity("MP_Prison", "Operation Locker", true);
        BattlefieldMapEntity metro = entity("XP0_Metro", "Operation Metro 2014", true);
        BattlefieldMap metroDomain = map("XP0_Metro", "Operation Metro 2014", true);
        BattlefieldMap lockerDomain = map("MP_Prison", "Operation Locker", true);

        when(repository.findAllEnabled()).thenReturn(List.of(locker, metro));
        when(mapper.toDomain(locker)).thenReturn(lockerDomain);
        when(mapper.toDomain(metro)).thenReturn(metroDomain);

        assertThat(service.listAvailableMaps())
                .extracting(BattlefieldMap::id)
                .containsExactly("MP_Prison", "XP0_Metro");
    }

    private BattlefieldMapEntity entity(String id, String displayName, boolean enabled) {
        Instant timestamp = Instant.parse("2026-08-28T12:00:00Z");
        return new BattlefieldMapEntity(
                id,
                displayName,
                enabled,
                "Base Game",
                Map.of(),
                timestamp,
                timestamp
        );
    }

    private BattlefieldMap map(String id, String displayName, boolean enabled) {
        return new BattlefieldMap(id, displayName, enabled, "Base Game", Map.of());
    }
}

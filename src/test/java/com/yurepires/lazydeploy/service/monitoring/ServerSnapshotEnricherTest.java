package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.server.MapSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerSnapshotEnricherTest {

    @Test
    void shouldEnrichKnownMapWithoutChangingTechnicalId() {
        MapCatalogService catalogService = mock(MapCatalogService.class);
        when(catalogService.isKnownMap("XP0_Metro")).thenReturn(true);
        when(catalogService.getDisplayName("XP0_Metro")).thenReturn("Operation Metro 2014");
        ServerSnapshotEnricher enricher = new ServerSnapshotEnricher(catalogService);

        ServerSnapshot enriched = enricher.enrich(snapshot("guid", "XP0_Metro", 48));

        assertThat(enriched.map().normalizedId()).isEqualTo("XP0_Metro");
        assertThat(enriched.map().displayName()).isEqualTo("Operation Metro 2014");
    }

    @Test
    void shouldFallbackToTechnicalIdForUnknownMap() {
        MapCatalogService catalogService = mock(MapCatalogService.class);
        when(catalogService.isKnownMap("XP9_NewMap")).thenReturn(false);
        when(catalogService.getDisplayName("XP9_NewMap")).thenReturn("XP9_NewMap");
        ServerSnapshotEnricher enricher = new ServerSnapshotEnricher(catalogService);

        ServerSnapshot source = new ServerSnapshot(
                "guid",
                new MapSnapshot("XP9_NewMap", "XP9_NewMap", null),
                snapshot("guid", "MAP_A", 1).players(),
                "CONQUEST",
                300,
                Instant.parse("2026-08-28T12:00:00Z"),
                Map.of()
        );

        assertThat(enricher.enrich(source).map().displayName()).isEqualTo("XP9_NewMap");
        assertThat(enricher.enrich(source).map().normalizedId()).isEqualTo("XP9_NewMap");
    }
}

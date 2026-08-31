package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.server.MapSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Acrescenta o nome amigável do catálogo sem alterar o identificador técnico
 * usado pelo monitoramento e pelas regras.
 */
@Component
public class ServerSnapshotEnricher {

    private static final Logger log = LoggerFactory.getLogger(ServerSnapshotEnricher.class);

    private final MapCatalogService mapCatalogService;

    public ServerSnapshotEnricher() {
        this.mapCatalogService = null;
    }

    @Autowired
    public ServerSnapshotEnricher(MapCatalogService mapCatalogService) {
        this.mapCatalogService = mapCatalogService;
    }

    public ServerSnapshot enrich(ServerSnapshot snapshot) {
        if (snapshot == null || snapshot.map() == null || mapCatalogService == null) {
            return snapshot;
        }

        MapSnapshot mapSnapshot = snapshot.map();
        String normalizedMapId = mapSnapshot.normalizedId();
        if (normalizedMapId == null || normalizedMapId.isBlank()) {
            return snapshot;
        }

        boolean knownMap = mapCatalogService.isKnownMap(normalizedMapId);
        String displayName = mapCatalogService.getDisplayName(normalizedMapId);
        if (!knownMap) {
            log.warn("Mapa não catalogado recebido do Keeper | mapId={}", normalizedMapId);
        }

        MapSnapshot enrichedMap = new MapSnapshot(
                mapSnapshot.externalId(),
                normalizedMapId,
                displayName
        );

        return new ServerSnapshot(
                snapshot.serverGuid(),
                enrichedMap,
                snapshot.players(),
                snapshot.gameMode(),
                snapshot.roundTimeSeconds(),
                snapshot.capturedAt(),
                snapshot.externalMetadata()
        );
    }
}

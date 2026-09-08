package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.server.MapSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
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
    private final LogSanitizer logSanitizer;

    public ServerSnapshotEnricher() {
        this(null, new LogSanitizer());
    }

    public ServerSnapshotEnricher(MapCatalogService mapCatalogService) {
        this(mapCatalogService, new LogSanitizer());
    }

    @Autowired
    public ServerSnapshotEnricher(
            MapCatalogService mapCatalogService,
            LogSanitizer logSanitizer
    ) {
        this.mapCatalogService = mapCatalogService;
        this.logSanitizer = logSanitizer;
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
            log.warn(
                    "Mapa não catalogado recebido do Keeper | mapId={}",
                    logSanitizer.sanitize(normalizedMapId)
            );
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

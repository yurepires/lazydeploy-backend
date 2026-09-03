package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.dto.response.CurrentMapResponse;
import com.yurepires.lazydeploy.dto.response.CurrentPlayersResponse;
import com.yurepires.lazydeploy.dto.response.CurrentServerStatusResponse;
import com.yurepires.lazydeploy.model.map.BattlefieldMap;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte o último estado persistido em um contrato seguro para a API.
 */
@Component
public class CurrentServerStatusMapper {

    private final MapCatalogService mapCatalogService;

    public CurrentServerStatusMapper(MapCatalogService mapCatalogService) {
        this.mapCatalogService = mapCatalogService;
    }

    public CurrentServerStatusResponse unavailable() {
        return new CurrentServerStatusResponse(
                false,
                "NOT_OBSERVED_YET",
                null,
                null,
                null,
                null,
                null
        );
    }

    public CurrentServerStatusResponse toResponse(
            ServerState state,
            Map<String, String> mapDisplayNames
    ) {
        if (state == null) {
            return unavailable();
        }

        if (state.mapId() == null || state.mapId().isBlank()) {
            return new CurrentServerStatusResponse(
                    false,
                    "INVALID_STATE",
                    null,
                    null,
                    state.gameMode(),
                    state.lastObservedAt(),
                    state.lastObservedAt()
            );
        }

        CurrentPlayersResponse players = null;
        if (state.playerCount() != null && state.maxPlayers() != null) {
            players = new CurrentPlayersResponse(
                    state.playerCount(),
                    state.maxPlayers()
            );
        }

        String mapId = state.mapId().trim();
        String displayName = mapId;
        if (mapDisplayNames != null && mapDisplayNames.containsKey(mapId)) {
            displayName = mapDisplayNames.get(mapId);
        }
        CurrentMapResponse map = new CurrentMapResponse(mapId, displayName);

        return new CurrentServerStatusResponse(
                true,
                null,
                map,
                players,
                state.gameMode(),
                state.lastObservedAt(),
                state.lastObservedAt()
        );
    }

    public Map<String, String> createMapDisplayNameIndex(List<BattlefieldMap> maps) {
        Map<String, String> displayNamesById = new HashMap<>();
        if (maps == null) {
            return displayNamesById;
        }

        for (BattlefieldMap map : maps) {
            if (map == null || map.id() == null || map.id().isBlank()) {
                continue;
            }

            String displayName = map.displayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = map.id();
            }
            displayNamesById.put(map.id().trim(), displayName);
        }

        return Map.copyOf(displayNamesById);
    }

    /**
     * Carrega o catálogo uma única vez para a consulta atual.
     */
    public Map<String, String> createMapDisplayNameIndex() {
        return createMapDisplayNameIndex(mapCatalogService.listAvailableMaps());
    }
}

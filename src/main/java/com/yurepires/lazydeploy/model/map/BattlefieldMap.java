package com.yurepires.lazydeploy.model.map;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadados de referência de um mapa do Battlefield 4.
 *
 * <p>O campo {@code id} é o identificador técnico normalizado recebido do
 * Battlelog Keeper. O nome amigável existe apenas para apresentação.</p>
 */
public record BattlefieldMap(
        String id,
        String displayName,
        boolean enabled,
        String expansion,
        Map<String, Object> metadata
) {

    public BattlefieldMap {
        metadata = metadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

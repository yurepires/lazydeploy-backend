package com.yurepires.lazydeploy.domain.server;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ServerCatalogSnapshot(
        List<ServerSnapshot> servers,
        Map<String, ServerSnapshot> byGuid,
        Map<ServerAddress, ServerSnapshot> byAddress,
        Instant capturedAt
) {
    public static ServerCatalogSnapshot from(List<ServerSnapshot> servers, Instant capturedAt) {
        Map<String, ServerSnapshot> byGuid = new LinkedHashMap<>();
        Map<ServerAddress, ServerSnapshot> byAddress = new LinkedHashMap<>();
        servers.forEach(server -> {
            if (server.externalGuid() != null && !server.externalGuid().isBlank()) {
                byGuid.putIfAbsent(server.externalGuid(), server);
            }
            if (server.address() != null) {
                byAddress.putIfAbsent(server.address(), server);
            }
        });
        return new ServerCatalogSnapshot(
                List.copyOf(servers), Map.copyOf(byGuid), Map.copyOf(byAddress), capturedAt
        );
    }
}

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
            if (server.serverGuid() != null && !server.serverGuid().isBlank()) {
                byGuid.putIfAbsent(server.serverGuid(), server);
            }
            Object address = server.externalMetadata().get("address");
            if (address instanceof String value && value.contains(":")) {
                int separator = value.lastIndexOf(':');
                byAddress.putIfAbsent(new ServerAddress(value.substring(0, separator), Integer.parseInt(value.substring(separator + 1))), server);
            }
        });
        return new ServerCatalogSnapshot(
                List.copyOf(servers), Map.copyOf(byGuid), Map.copyOf(byAddress), capturedAt
        );
    }
}

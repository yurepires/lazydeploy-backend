package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Bf4ServerSnapshot(
        List<Bf4ServerResponse> servers,
        Map<String, Bf4ServerResponse> byGuid,
        Map<String, Bf4ServerResponse> byAddress,
        Instant fetchedAt
) {

    public Bf4ServerSnapshot {
        servers = List.copyOf(servers);
        byGuid = Map.copyOf(byGuid);
        byAddress = Map.copyOf(byAddress);
    }

    public static Bf4ServerSnapshot from(List<Bf4ServerResponse> servers, Instant fetchedAt) {
        Map<String, Bf4ServerResponse> byGuid = new LinkedHashMap<>();
        Map<String, Bf4ServerResponse> byAddress = new LinkedHashMap<>();

        for (Bf4ServerResponse server : servers) {
            if (server.guid() != null && !server.guid().isBlank()) {
                byGuid.putIfAbsent(server.guid(), server);
            }
            if (server.ip() != null && !server.ip().isBlank()) {
                byAddress.putIfAbsent(addressKey(server.ip(), server.port()), server);
            }
        }

        return new Bf4ServerSnapshot(servers, byGuid, byAddress, fetchedAt);
    }

    public Optional<Bf4ServerResponse> find(Bf4Properties.MonitoredServer target) {
        Bf4ServerResponse guidMatch = byGuid.get(target.guid());
        if (guidMatch != null) {
            return Optional.of(guidMatch);
        }

        if (target.ip() == null || target.ip().isBlank() || target.port() <= 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(byAddress.get(addressKey(target.ip(), target.port())));
    }

    private static String addressKey(String ip, int port) {
        return ip + ":" + port;
    }
}

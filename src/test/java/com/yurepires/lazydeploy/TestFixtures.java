package com.yurepires.lazydeploy;

import com.yurepires.lazydeploy.domain.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.domain.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.domain.server.MapSnapshot;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.PlayerSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerIdentifiers;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class TestFixtures {
    private TestFixtures() {}

    public static MonitoredServer monitored(
            String id,
            String guid,
            List<NotificationRuleDefinition> rules,
            List<NotificationChannelConfiguration> channels
    ) {
        return new MonitoredServer(
                id,
                new ServerIdentifiers(guid, "10.0.0.1", 25226),
                "Server " + id,
                true,
                rules,
                channels
        );
    }

    public static ServerSnapshot snapshot(String guid, String map, int players) {
        return new ServerSnapshot(
                guid,
                new MapSnapshot(map, map, map),
                new PlayerSnapshot(players, 64, 0),
                "CONQUEST",
                300,
                Instant.parse("2026-08-12T12:00:00Z"),
                Map.of("address", "10.0.0.1:25226", "name", "Server")
        );
    }

    public static NotificationRuleDefinition rule(String type, Map<String, Object> parameters) {
        return new NotificationRuleDefinition(type, true, parameters);
    }
}

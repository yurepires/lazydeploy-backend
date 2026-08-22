package com.yurepires.lazydeploy.model.persistence;

import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.model.server.MonitoredServer;
import com.yurepires.lazydeploy.model.server.ServerIdentifiers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServerSubscription(
        UUID id,
        UUID userId,
        Server server,
        String externalGuid,
        boolean enabled,
        List<NotificationRuleDefinition> rules,
        List<NotificationChannelConfiguration> channels,
        Instant createdAt,
        Instant updatedAt
) {
    public ServerSubscription {
        rules = rules == null ? List.of() : List.copyOf(rules);
        channels = channels == null ? List.of() : List.copyOf(channels);
    }

    public MonitoredServer toMonitoredServer() {
        return new MonitoredServer(
                id.toString(), new ServerIdentifiers(externalGuid, null, null), server.displayName(),
                enabled && server.enabled(), rules, channels
        );
    }
}

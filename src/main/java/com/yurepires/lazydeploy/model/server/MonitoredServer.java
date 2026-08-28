package com.yurepires.lazydeploy.model.server;

import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;

import java.util.List;
import java.util.UUID;

public record MonitoredServer(
        String id,
        ServerIdentifiers identifiers,
        String displayName,
        boolean enabled,
        List<NotificationRuleDefinition> rules,
        List<NotificationChannelConfiguration> notificationChannels,
        UUID serverId
) {
    public MonitoredServer(
            String id,
            ServerIdentifiers identifiers,
            String displayName,
            boolean enabled,
            List<NotificationRuleDefinition> rules,
            List<NotificationChannelConfiguration> notificationChannels
    ) {
        this(
                id,
                identifiers,
                displayName,
                enabled,
                rules,
                notificationChannels,
                null
        );
    }

    public MonitoredServer {
        rules = rules == null ? List.of() : List.copyOf(rules);
        notificationChannels = notificationChannels == null ? List.of() : List.copyOf(notificationChannels);
    }
}

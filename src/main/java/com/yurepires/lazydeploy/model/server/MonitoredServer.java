package com.yurepires.lazydeploy.model.server;

import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;

import java.util.List;

public record MonitoredServer(
        String id,
        ServerIdentifiers identifiers,
        String displayName,
        boolean enabled,
        List<NotificationRuleDefinition> rules,
        List<NotificationChannelConfiguration> notificationChannels
) {
    public MonitoredServer {
        rules = rules == null ? List.of() : List.copyOf(rules);
        notificationChannels = notificationChannels == null ? List.of() : List.copyOf(notificationChannels);
    }
}

package com.yurepires.lazydeploy.domain.server;

import com.yurepires.lazydeploy.domain.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.domain.rule.NotificationRuleDefinition;

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

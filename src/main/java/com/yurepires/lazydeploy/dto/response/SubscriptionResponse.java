package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID userId,
        UUID serverId,
        String serverGuid,
        String displayName,
        boolean enabled,
        List<NotificationRuleDefinition> rules,
        List<NotificationChannelConfiguration> channels,
        Instant createdAt,
        Instant updatedAt
) {

    public SubscriptionResponse {
        if (rules == null) {
            rules = List.of();
        } else {
            rules = List.copyOf(rules);
        }

        if (channels == null) {
            channels = List.of();
        } else {
            channels = List.copyOf(channels);
        }
    }

    public static SubscriptionResponse from(ServerSubscription subscription) {
        return new SubscriptionResponse(
                subscription.id(),
                subscription.userId(),
                subscription.server().id(),
                subscription.externalGuid(),
                subscription.server().displayName(),
                subscription.enabled(),
                subscription.rules(),
                subscription.channels(),
                subscription.createdAt(),
                subscription.updatedAt()
        );
    }
}

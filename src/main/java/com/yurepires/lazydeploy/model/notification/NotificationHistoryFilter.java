package com.yurepires.lazydeploy.model.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationHistoryFilter(
        UUID subscriptionId,
        UUID serverId,
        NotificationDeliveryStatus status,
        String channelType,
        String mapId,
        Instant from,
        Instant to
) {
    public NotificationHistoryFilter withSubscriptionId(UUID subscription) {
        return new NotificationHistoryFilter(
                subscription,
                serverId,
                status,
                channelType,
                mapId,
                from,
                to
        );
    }
}

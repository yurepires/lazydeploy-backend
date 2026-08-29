package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        String type,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public ChannelResponse(
            UUID id,
            String type,
            boolean enabled,
            Map<String, Object> ignoredParameters,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, type, enabled, createdAt, updatedAt);
    }

    public static ChannelResponse from(NotificationChannelConfiguration channel) {
        return new ChannelResponse(
                channel.id(),
                channel.type(),
                channel.enabled(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}

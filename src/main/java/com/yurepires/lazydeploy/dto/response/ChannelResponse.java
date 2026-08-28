package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        String type,
        boolean enabled,
        Map<String, Object> parameters,
        Instant createdAt,
        Instant updatedAt
) {

    public ChannelResponse {
        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Map.copyOf(parameters);
        }
    }

    public static ChannelResponse from(NotificationChannelConfiguration channel) {
        return new ChannelResponse(
                channel.id(),
                channel.type(),
                channel.enabled(),
                channel.parameters(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}

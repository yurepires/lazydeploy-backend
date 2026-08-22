package com.yurepires.lazydeploy.model.notification;

import com.yurepires.lazydeploy.model.server.ServerSnapshot;

import java.time.Instant;
import java.util.Map;

public record NotificationCandidate(
        String serverId,
        ServerSnapshot server,
        String stateIdentity,
        NotificationDecision decision,
        Instant createdAt,
        Map<String, Object> attributes
) {
    public NotificationCandidate {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}

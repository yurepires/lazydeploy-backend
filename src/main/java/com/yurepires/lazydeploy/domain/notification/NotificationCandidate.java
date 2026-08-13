package com.yurepires.lazydeploy.domain.notification;

import com.yurepires.lazydeploy.domain.server.ServerSnapshot;

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

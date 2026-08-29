package com.yurepires.lazydeploy.model.notification;

import com.yurepires.lazydeploy.model.server.ServerSnapshot;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationCandidate(
        String serverId,
        ServerSnapshot server,
        String stateIdentity,
        NotificationDecision decision,
        Instant createdAt,
        Map<String, Object> attributes,
        UUID userId
) {
    public NotificationCandidate(
            String serverId,
            ServerSnapshot server,
            String stateIdentity,
            NotificationDecision decision,
            Instant createdAt,
            Map<String, Object> attributes
    ) {
        this(serverId, server, stateIdentity, decision, createdAt, attributes, null);
    }

    public NotificationCandidate {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}

package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.persistence.Server;

import java.util.UUID;

public record ServerReferenceResponse(
        UUID id,
        String guid,
        String displayName
) {

    public static ServerReferenceResponse from(Server server, String guid) {
        return new ServerReferenceResponse(server.id(), guid, server.displayName());
    }
}

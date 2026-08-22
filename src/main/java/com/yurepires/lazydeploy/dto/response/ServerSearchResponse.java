package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.server.ServerReference;

import java.util.Map;

public record ServerSearchResponse(
        String externalGuid,
        String displayName,
        String provider,
        Map<String, Object> metadata
) {
    public ServerSearchResponse {
        if (metadata == null) {
            metadata = Map.of();
        } else {
            metadata = Map.copyOf(metadata);
        }
    }

    public static ServerSearchResponse from(ServerReference serverReference) {
        return new ServerSearchResponse(
                serverReference.externalGuid(),
                serverReference.displayName(),
                serverReference.provider(),
                serverReference.metadata()
        );
    }
}

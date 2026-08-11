package com.yurepires.lazydeploy.dto;

import java.util.List;

public record Bf4ServerPageResponse(
        List<Bf4ServerResponse> servers,
        String cursor,
        boolean hasMore
) {
    public Bf4ServerPageResponse {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }
}

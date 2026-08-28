package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.map.BattlefieldMap;

public record BattlefieldMapResponse(
        String id,
        String displayName,
        String expansion
) {

    public static BattlefieldMapResponse from(BattlefieldMap map) {
        return new BattlefieldMapResponse(
                map.id(),
                map.displayName(),
                map.expansion()
        );
    }
}

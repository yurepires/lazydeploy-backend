package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.domain.server.MapSnapshot;
import com.yurepires.lazydeploy.domain.server.PlayerSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.battlefield.dto.Bf4ServerResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class BfListServerMapper {

    public ServerSnapshot map(Bf4ServerResponse response, Instant capturedAt) {
        return new ServerSnapshot(
                response.guid(),
                new MapSnapshot(response.map(), response.map(), response.mapLabel()),
                new PlayerSnapshot(response.numPlayers(), response.maxPlayers(), 0),
                response.gameType(),
                response.roundTime(),
                capturedAt,
                Map.of("address", response.ip() + ":" + response.port(), "name", response.name())
        );
    }
}

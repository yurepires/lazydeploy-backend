package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.domain.server.MapSnapshot;
import com.yurepires.lazydeploy.domain.server.PlayerSnapshot;
import com.yurepires.lazydeploy.domain.server.RoundSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerAddress;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.battlefield.dto.Bf4ServerResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BfListServerMapper {

    public ServerSnapshot map(Bf4ServerResponse response, Instant capturedAt) {
        return new ServerSnapshot(
                response.guid(),
                new ServerAddress(response.ip(), response.port()),
                response.name(),
                new MapSnapshot(response.map(), response.mapLabel()),
                new PlayerSnapshot(response.numPlayers(), response.maxPlayers()),
                response.gameType(),
                new RoundSnapshot(
                        response.roundsPlayed(), response.roundsTotal(), response.roundTime()
                ),
                capturedAt
        );
    }
}

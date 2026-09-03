package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.NotificationStateEntity;
import com.yurepires.lazydeploy.entity.RoundInstanceEntity;
import com.yurepires.lazydeploy.entity.ServerStateEntity;
import com.yurepires.lazydeploy.model.monitoring.NotificationState;
import com.yurepires.lazydeploy.model.monitoring.RoundInstance;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = LazyDeployMapperConfig.class)
public interface MonitoringMapper {

    @Mapping(target = "attemptedAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    NotificationState toDomain(NotificationStateEntity notificationStateEntity);

    NotificationStateEntity toEntity(NotificationState notificationState);

    RoundInstance toDomain(RoundInstanceEntity roundInstanceEntity);

    RoundInstanceEntity toEntity(RoundInstance roundInstance);

    ServerState toDomain(ServerStateEntity serverStateEntity);

    default ServerStateEntity toEntity(ServerState serverState) {
        if (serverState == null) {
            return null;
        }

        return new ServerStateEntity(
                serverState.serverId(),
                serverState.roundInstanceId(),
                serverState.mapId(),
                serverState.previousRoundTimeSeconds(),
                serverState.roundDetectedAt(),
                serverState.lastObservedAt(),
                serverState.playerCount(),
                serverState.maxPlayers(),
                serverState.gameMode()
        );
    }
}

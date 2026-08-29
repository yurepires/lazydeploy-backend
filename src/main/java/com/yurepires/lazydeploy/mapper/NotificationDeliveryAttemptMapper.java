package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.model.persistence.NotificationDeliveryAttempt;
import org.mapstruct.Mapper;

@Mapper(config = LazyDeployMapperConfig.class)
public interface NotificationDeliveryAttemptMapper {

    NotificationDeliveryAttempt toDomain(NotificationDeliveryAttemptEntity deliveryAttemptEntity);

    default NotificationDeliveryAttemptEntity toEntity(NotificationDeliveryAttempt deliveryAttempt) {
        if (deliveryAttempt == null) {
            return null;
        }

        return new NotificationDeliveryAttemptEntity(
                deliveryAttempt.id(),
                deliveryAttempt.subscriptionId(),
                deliveryAttempt.ownerUserId(),
                deliveryAttempt.roundInstanceId(),
                deliveryAttempt.serverId(),
                deliveryAttempt.serverDisplayName(),
                deliveryAttempt.mapId(),
                deliveryAttempt.mapDisplayName(),
                deliveryAttempt.playerCount(),
                deliveryAttempt.maxPlayers(),
                deliveryAttempt.gameMode(),
                deliveryAttempt.recipientSnapshot(),
                deliveryAttempt.channelType(),
                deliveryAttempt.status(),
                deliveryAttempt.attemptedAt(),
                deliveryAttempt.sentAt(),
                deliveryAttempt.errorCode(),
                deliveryAttempt.errorMessage(),
                deliveryAttempt.metadata()
        );
    }
}

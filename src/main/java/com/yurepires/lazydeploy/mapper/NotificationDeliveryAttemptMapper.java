package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.model.persistence.NotificationDeliveryAttempt;
import org.mapstruct.Mapper;

@Mapper(config = LazyDeployMapperConfig.class)
public interface NotificationDeliveryAttemptMapper {

    NotificationDeliveryAttempt toDomain(NotificationDeliveryAttemptEntity deliveryAttemptEntity);

    NotificationDeliveryAttemptEntity toEntity(NotificationDeliveryAttempt deliveryAttempt);
}

package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.model.notification.NotificationDeliveryStatus;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;
import org.mapstruct.Mapper;

import java.util.Locale;

@Mapper(config = LazyDeployMapperConfig.class)
public interface NotificationHistoryMapper {

    NotificationHistoryEntry toDomain(NotificationDeliveryAttemptEntity entity);

    default NotificationDeliveryStatus toStatus(String status) {
        if (status == null) {
            return null;
        }
        return NotificationDeliveryStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }
}

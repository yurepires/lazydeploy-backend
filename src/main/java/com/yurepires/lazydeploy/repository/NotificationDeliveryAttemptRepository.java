package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttemptEntity, UUID> {

}

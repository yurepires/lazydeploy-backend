package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.NotificationChannelConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationChannelConfigurationRepository extends JpaRepository<NotificationChannelConfigurationEntity, UUID> {

    List<NotificationChannelConfigurationEntity> findAllBySubscriptionId(UUID subscriptionId);

    void deleteAllBySubscriptionId(UUID subscriptionId);
}

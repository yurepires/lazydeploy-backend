package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.NotificationStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationStateRepository extends JpaRepository<NotificationStateEntity, UUID> {

    Optional<NotificationStateEntity> findBySubscriptionIdAndRoundInstanceId(UUID subscriptionId, UUID roundInstanceId);

    void deleteAllBySubscriptionId(UUID subscriptionId);

}

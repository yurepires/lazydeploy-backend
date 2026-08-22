package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.NotificationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, UUID> {

    List<NotificationRuleEntity> findAllBySubscriptionId(UUID subscriptionId);

    void deleteAllBySubscriptionId(UUID subscriptionId);
}

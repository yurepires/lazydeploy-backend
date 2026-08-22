package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.NotificationRuleParameterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRuleParameterRepository extends JpaRepository<NotificationRuleParameterEntity, UUID> {

    List<NotificationRuleParameterEntity> findAllByRuleId(UUID ruleId);

    void deleteAllByRuleIdIn(List<UUID> ruleIds);
}

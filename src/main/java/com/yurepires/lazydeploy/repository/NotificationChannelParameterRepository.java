package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.NotificationChannelParameterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationChannelParameterRepository extends JpaRepository<NotificationChannelParameterEntity, UUID> {

    List<NotificationChannelParameterEntity> findAllByChannelConfigurationId(UUID channelConfigurationId);

    void deleteAllByChannelConfigurationIdIn(List<UUID> channelConfigurationIds);
}

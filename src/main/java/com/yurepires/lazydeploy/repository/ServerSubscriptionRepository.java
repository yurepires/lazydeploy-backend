package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.ServerSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServerSubscriptionRepository extends JpaRepository<ServerSubscriptionEntity, UUID> {

    List<ServerSubscriptionEntity> findAllByEnabledTrue();

    List<ServerSubscriptionEntity> findAllByUserId(UUID userId);

    Optional<ServerSubscriptionEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ServerSubscriptionEntity> findByUserIdAndServerId(UUID userId, UUID serverId);
}

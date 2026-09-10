package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.ServerSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServerSubscriptionRepository extends JpaRepository<ServerSubscriptionEntity, UUID> {

    List<ServerSubscriptionEntity> findAllByEnabledTrue();

    List<ServerSubscriptionEntity> findAllByUserId(UUID userId);

    long countByUserId(UUID userId);

    Optional<ServerSubscriptionEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ServerSubscriptionEntity> findByUserIdAndServerId(UUID userId, UUID serverId);

    boolean existsByUserIdAndServerId(UUID userId, UUID serverId);

    @Modifying
    @Query("""
            update ServerSubscriptionEntity subscription
            set subscription.updatedAt = :updatedAt
            where subscription.id = :subscriptionId
            """)
    int updateUpdatedAt(
            @Param("subscriptionId") UUID subscriptionId,
            @Param("updatedAt") Instant updatedAt
    );
}

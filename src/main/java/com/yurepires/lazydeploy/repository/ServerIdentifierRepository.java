package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.ServerIdentifierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServerIdentifierRepository extends JpaRepository<ServerIdentifierEntity, UUID> {

    Optional<ServerIdentifierEntity> findByProviderAndTypeAndValue(String provider, String type, String value);

    Optional<ServerIdentifierEntity> findByServerIdAndProviderAndType(UUID serverId, String provider, String type);
}

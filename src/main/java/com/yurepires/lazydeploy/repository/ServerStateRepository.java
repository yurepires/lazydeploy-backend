package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.ServerStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ServerStateRepository extends JpaRepository<ServerStateEntity, UUID> {

    List<ServerStateEntity> findAllByServerIdIn(Collection<UUID> serverIds);
}

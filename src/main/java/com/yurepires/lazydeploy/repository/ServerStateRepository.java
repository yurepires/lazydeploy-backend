package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.ServerStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServerStateRepository extends JpaRepository<ServerStateEntity, UUID> {

}

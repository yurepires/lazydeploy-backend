package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.ServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServerRepository extends JpaRepository<ServerEntity, UUID> {

}

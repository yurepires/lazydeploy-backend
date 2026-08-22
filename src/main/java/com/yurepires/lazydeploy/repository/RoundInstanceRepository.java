package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.RoundInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoundInstanceRepository extends JpaRepository<RoundInstanceEntity, UUID> {

}

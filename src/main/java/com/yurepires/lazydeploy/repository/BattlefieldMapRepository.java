package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.BattlefieldMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BattlefieldMapRepository extends JpaRepository<BattlefieldMapEntity, String> {

    List<BattlefieldMapEntity> findAllByEnabledTrue();

    List<BattlefieldMapEntity> findAllByEnabledTrueOrderByDisplayNameAsc();

    default List<BattlefieldMapEntity> findAllEnabled() {
        return findAllByEnabledTrueOrderByDisplayNameAsc();
    }
}

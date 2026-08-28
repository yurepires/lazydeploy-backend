package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.BattlefieldMapEntity;
import com.yurepires.lazydeploy.model.map.BattlefieldMap;
import org.mapstruct.Mapper;

@Mapper(config = LazyDeployMapperConfig.class)
public interface BattlefieldMapMapper {

    BattlefieldMap toDomain(BattlefieldMapEntity entity);
}

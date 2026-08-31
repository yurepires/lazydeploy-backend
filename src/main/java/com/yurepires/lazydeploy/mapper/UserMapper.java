package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.model.persistence.User;
import org.mapstruct.Mapper;

@Mapper(config = LazyDeployMapperConfig.class)
public interface UserMapper {

    User toDomain(UserEntity userEntity);

}

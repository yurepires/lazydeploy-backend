package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.model.persistence.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = LazyDeployMapperConfig.class)
public interface UserMapper {

    User toDomain(UserEntity userEntity);

    @Mapping(target = "passwordHash", ignore = true)
    UserEntity toEntity(User user);

}

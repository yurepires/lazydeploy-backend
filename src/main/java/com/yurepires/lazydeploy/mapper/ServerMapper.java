package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.ServerEntity;
import com.yurepires.lazydeploy.entity.ServerIdentifierEntity;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerIdentifier;
import org.mapstruct.Mapper;

@Mapper(config = LazyDeployMapperConfig.class)
public interface ServerMapper {

    Server toDomain(ServerEntity serverEntity);

    ServerEntity toEntity(Server server);

    ServerIdentifier toDomain(ServerIdentifierEntity identifierEntity);

    ServerIdentifierEntity toEntity(ServerIdentifier serverIdentifier);
}

package com.yurepires.lazydeploy.service.subscription;

import com.yurepires.lazydeploy.model.persistence.ExternalIdentifierTypes;
import com.yurepires.lazydeploy.mapper.ServerMapper;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerIdentifier;
import com.yurepires.lazydeploy.repository.ServerRepository;
import com.yurepires.lazydeploy.repository.ServerIdentifierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class NewServerTransaction {

    private final ServerRepository serverRepository;
    private final ServerIdentifierRepository identifierRepository;
    private final ServerMapper serverMapper;
    private final Clock clock;

    public NewServerTransaction(
            ServerRepository serverRepository,
            ServerIdentifierRepository identifierRepository,
            ServerMapper serverMapper,
            Clock clock
    ) {
        this.serverRepository = serverRepository;
        this.identifierRepository = identifierRepository;
        this.serverMapper = serverMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Server create(String guid, String displayName) {
        Instant currentTime = Instant.now(clock);
        Server server = new Server(
                UUID.randomUUID(),
                displayName,
                true,
                currentTime,
                currentTime
        );
        Server savedServer = serverMapper.toDomain(
                serverRepository.save(serverMapper.toEntity(server))
        );

        ServerIdentifier identifier = new ServerIdentifier(
                UUID.randomUUID(),
                savedServer.id(),
                ExternalIdentifierTypes.BATTLELOG,
                ExternalIdentifierTypes.GUID,
                guid
        );
        identifierRepository.save(serverMapper.toEntity(identifier));

        return savedServer;
    }
}

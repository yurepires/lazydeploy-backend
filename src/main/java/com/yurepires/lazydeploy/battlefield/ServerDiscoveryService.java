package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ServerDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ServerDiscoveryService.class);

    private final BfListClient bfListClient;
    private final Bf4Properties properties;

    public ServerDiscoveryService(BfListClient bfListClient, Bf4Properties properties) {
        this.bfListClient = bfListClient;
        this.properties = properties;
    }

    public Optional<Bf4ServerResponse> findTrackedServer() {
        Bf4Properties.Server target = properties.server();

        log.info("Buscando servidor configurado no snapshot atual do BFLIST...");

        Optional<Bf4ServerResponse> server = bfListClient.findServer(target.guid());

        server.ifPresent(found -> log.info(
                "Servidor configurado encontrado: {} | {}:{} | GUID={}",
                found.name(),
                found.ip(),
                found.port(),
                found.guid()
        ));

        return server;
    }
}

package com.yurepires.lazydeploy.domain.server;

import java.util.Optional;

public interface ServerLocator {
    Optional<ServerSnapshot> find(MonitoredServer monitoredServer, ServerCatalogSnapshot snapshot);
}

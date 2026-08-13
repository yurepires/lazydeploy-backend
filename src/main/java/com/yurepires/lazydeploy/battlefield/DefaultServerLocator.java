package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerCatalogSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerIdentifiers;
import com.yurepires.lazydeploy.domain.server.ServerLocator;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DefaultServerLocator implements ServerLocator {

    @Override
    public Optional<ServerSnapshot> find(MonitoredServer monitoredServer, ServerCatalogSnapshot snapshot) {
        ServerIdentifiers identifiers = monitoredServer.identifiers();
        if (identifiers.hasGuid()) {
            ServerSnapshot match = snapshot.byGuid().get(identifiers.guid());
            if (match != null) {
                return Optional.of(match);
            }
        }

        return identifiers.hasAddress() ? Optional.ofNullable(snapshot.byAddress().get(identifiers.address())) : Optional.empty();
    }
}

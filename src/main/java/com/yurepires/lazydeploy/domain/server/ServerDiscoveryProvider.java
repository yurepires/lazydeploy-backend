package com.yurepires.lazydeploy.domain.server;

import java.util.List;

public interface ServerDiscoveryProvider {
    List<ServerReference> search(ServerSearchQuery query);

    String providerId();
}

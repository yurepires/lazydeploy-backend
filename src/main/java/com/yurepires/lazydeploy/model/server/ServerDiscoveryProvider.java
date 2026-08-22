package com.yurepires.lazydeploy.model.server;

import java.util.List;

public interface ServerDiscoveryProvider {
    List<ServerReference> search(ServerSearchQuery query);

    String providerId();
}

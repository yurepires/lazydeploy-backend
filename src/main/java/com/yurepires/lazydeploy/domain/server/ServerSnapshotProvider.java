package com.yurepires.lazydeploy.domain.server;

import java.util.Optional;

public interface ServerSnapshotProvider {
    Optional<ServerSnapshot> getSnapshot(ServerReference server);

    String providerId();
}
